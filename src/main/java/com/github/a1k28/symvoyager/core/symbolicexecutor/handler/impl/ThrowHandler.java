package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.HandlerNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ThrowHandler extends AbstractSymbolicHandler {

    public ThrowHandler(SymbolicHandlerContext hc) {
        super(hc);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        Class<?> exceptionType = methodPath.getSymbolicVarStack()
                .get(hc.getZ3t().getValueName(((JThrowStmt) stmt).getOp())).orElseThrow()
                .getClassType();

        HandlerNode handlerNode = findHandlerNode(
                methodPath, methodPath.getBlock(stmt), exceptionType);
        if (handlerNode == null) return SType.THROW_END;

        BasicBlock<?> block = handlerNode.getBlock();
        Map<ClassType, BasicBlock<?>> exceptionalSuccessors
                = (Map<ClassType, BasicBlock<?>>) block.getExceptionalSuccessors();
        SortedSet<BasicBlock<?>> chosen = new TreeSet<>(SootInterpreter::compareBlocks);

        for (Map.Entry<ClassType, BasicBlock<?>> entry : exceptionalSuccessors.entrySet()) {
            Class<?> classType = SootInterpreter.getClass(entry.getKey());
            if (classType.isAssignableFrom(exceptionType)) {
                chosen.add(entry.getValue());
            }
        }

        if (chosen.isEmpty()) return SType.THROW_END;

        hc.getSe().push(handlerNode.getMethodPath());
        hc.getSe().analyzePaths(handlerNode.getMethodPath(), chosen.iterator().next());
        hc.getSe().pop(handlerNode.getMethodPath());
        return SType.THROW;
    }

    private HandlerNode findHandlerNode(SMethodPath methodPath, BasicBlock<?> block, Class<?> exceptionType) {
        Map<ClassType, BasicBlock<?>> exceptionalSuccessors
                = (Map<ClassType, BasicBlock<?>>) block.getExceptionalSuccessors();

        boolean match = false;
        for (Map.Entry<ClassType, BasicBlock<?>> entry : exceptionalSuccessors.entrySet()) {
            Class<?> classType = SootInterpreter.getClass(entry.getKey());
            if (classType.isAssignableFrom(exceptionType)) {
                match = true;
                break;
            }
        }

        if (match) {
            return new HandlerNode(methodPath, block);
        } else if (block.getPredecessors().size() == 1) {
            return findHandlerNode(methodPath, block.getPredecessors().get(0), exceptionType);
        } else if (methodPath.getJumpNode() != null) {
            return findHandlerNode(
                    methodPath.getJumpNode().getMethodPath(),
                    methodPath.getJumpNode().getBlock(),
                    exceptionType);
        }

        return null;
    }
}
