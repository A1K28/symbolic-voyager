package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
import com.microsoft.z3.BoolExpr;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JIfStmt;

public class BranchHandler extends AbstractSymbolicHandler {
    public BranchHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        BoolExpr z3Condition = (BoolExpr) hc.getZ3t().translateCondition(
                condition, getVarType(methodPath, ifStmt), methodPath);
        BoolExpr assertion = node.getType() == SType.BRANCH_TRUE ?
                z3Condition : hc.getCtx().mkNot(z3Condition);
        hc.getSe().addAssertion(assertion);
        return SType.BRANCH;
    }
}
