package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.microsoft.z3.BoolExpr;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;

public class BranchHandler extends AbstractSymbolicHandler {
    public BranchHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        JIfStmt ifStmt = (JIfStmt) stmt;
        Value condition = ifStmt.getCondition();
        BoolExpr assertionTrue = (BoolExpr) hc.getZ3t().translateCondition(
                condition, getVarType(methodPath, ifStmt), methodPath);
        BoolExpr assertionFalse = hc.getCtx().mkNot(assertionTrue);

        BasicBlock<?> block = methodPath.getBlock(stmt);
        assert block.getSuccessors().size() == 2;

        hc.getSe().push(methodPath);
        hc.getSe().addAssertion(assertionTrue);
        hc.getSe().analyzePaths(methodPath, block.getSuccessors().get(1));
        hc.getSe().pop(methodPath);

        hc.getSe().push(methodPath);
        hc.getSe().addAssertion(assertionFalse);
        hc.getSe().analyzePaths(methodPath, block.getSuccessors().get(0));
        hc.getSe().pop(methodPath);

        return SType.BRANCH;
    }
}
