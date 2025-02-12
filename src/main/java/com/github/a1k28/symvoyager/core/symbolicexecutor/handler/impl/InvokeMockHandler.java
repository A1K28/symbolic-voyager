package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.BasicExceptionBlock;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.microsoft.z3.Expr;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.util.*;

// expand paths by allowing method mocks to throw exceptions
public class InvokeMockHandler extends AbstractSymbolicHandler {
    public InvokeMockHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        if (CLIOptions.disableMockExploration)
            return SType.OTHER;

        String refName;
        if (stmt instanceof JAssignStmt assignStmt)
            refName = hc.getZ3t().getValueName(assignStmt.getLeftOp());
        else
            refName = stmt.toString();

        Expr mockReferenceExpr = methodPath.getSymbolicVarStack().get(refName)
                .orElseGet(() -> methodPath.getMethodMockStack().get(refName)
                        .orElseThrow()).getExpr();

        SortedSet<BasicExceptionBlock> handlerNodes = findHandlerNodes(methodPath, methodPath.getBlock(stmt));
        for (BasicExceptionBlock beb : handlerNodes) {
            hc.getSe().push(methodPath);
            hc.getCtx().getMethodMockInstance().setExceptionType(mockReferenceExpr, beb.getExceptionType());
            hc.getSe().analyzePaths(beb.getMethodPath(), beb.getBlock());
            hc.getSe().pop(methodPath);
        }

        // clear throws so that further paths assume a return value (if non-void)
        hc.getCtx().getMethodMockInstance().setExceptionType(mockReferenceExpr, null);
        return SType.OTHER;
    }

    private SortedSet<BasicExceptionBlock> findHandlerNodes(SMethodPath methodPath, BasicBlock<?> block) {
        SortedSet<BasicExceptionBlock> set = new TreeSet<>(this::compareExceptionBlocks);
        findHandlerNodes(set, methodPath, block);

        List<BasicExceptionBlock> sorted = new ArrayList<>(set);

        // cleanup - remove unreachable nodes
        Set<BasicExceptionBlock> toRemove = new HashSet<>();
        outer: for (int i = sorted.size()-1; i>=0; i--) {
            Class<?> exceptionType = SootInterpreter.getClass(sorted.get(i).getExceptionType());
            for (int j = 0; j < i; j++) {
                Class<?> sup = SootInterpreter.getClass(sorted.get(j).getExceptionType());
                if (sup.isAssignableFrom(exceptionType)) {
                    toRemove.add(sorted.get(i));
                    continue outer;
                }
            }
        }

        set.removeAll(toRemove);
        return set;
    }

    private void findHandlerNodes(SortedSet<BasicExceptionBlock> set,
                                  SMethodPath methodPath,
                                  BasicBlock<?> block) {
        Map<ClassType, BasicBlock<?>> exceptionalSuccessors
                = (Map<ClassType, BasicBlock<?>>) block.getExceptionalSuccessors();

        for (Map.Entry<ClassType, BasicBlock<?>> entry : exceptionalSuccessors.entrySet()) {
            set.add(new BasicExceptionBlock(entry.getKey(), entry.getValue(), methodPath));
        }

        if (block.getPredecessors().size() == 1) {
            findHandlerNodes(set, methodPath, block.getPredecessors().get(0));
        } else if (methodPath.getJumpNode() != null) {
            findHandlerNodes(set,
                    methodPath.getJumpNode().getMethodPath(),
                    methodPath.getJumpNode().getBlock());
        }
    }

    private int compareExceptionBlocks(BasicExceptionBlock e1, BasicExceptionBlock e2) {
        return SootInterpreter.compareBlocks(e1.getBlock(), e2.getBlock());
    }
}
