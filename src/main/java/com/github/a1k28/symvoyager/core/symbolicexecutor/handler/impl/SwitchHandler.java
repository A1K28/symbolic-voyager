package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.microsoft.z3.BoolExpr;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;

import java.util.ArrayList;
import java.util.List;

public class SwitchHandler extends AbstractSymbolicHandler {
    public SwitchHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        BasicBlock<?> block = methodPath.getBlock(stmt);
        JSwitchStmt switchStmt = (JSwitchStmt) stmt;
        assert Math.abs(switchStmt.getValueCount() - block.getSuccessors().size()) <= 1;
        List<BoolExpr> assertions = new ArrayList<>();
        List<IntConstant> values = switchStmt.getValues();
        for (int i = 0; i < block.getSuccessors().size(); i++) {
            if (i < values.size()) {
                JEqExpr eqExpr = Jimple.newEqExpr(switchStmt.getKey(), values.get(i));
                BoolExpr assertionTrue = (BoolExpr) hc.getZ3t().translateCondition(
                        eqExpr, getVarType(methodPath, eqExpr), methodPath);
                assertions.add(assertionTrue);
            }

            hc.getSe().push(methodPath);
            if (i < values.size()) {
                hc.getSe().addAssertion(assertions.get(i));
            } else {
                for (int k = 0; k < i; k++)
                    hc.getSe().addAssertion(hc.getCtx().mkNot(assertions.get(k)));
            }
            hc.getSe().analyzePaths(methodPath, block.getSuccessors().get(i));
            hc.getSe().pop(methodPath);
        }
        return SType.SWITCH;
    }
}
