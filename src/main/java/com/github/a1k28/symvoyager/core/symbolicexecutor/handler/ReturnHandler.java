package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.microsoft.z3.Expr;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;

public class ReturnHandler extends AbstractSymbolicHandler {
    public ReturnHandler(SymbolicHandlerContext hc) {
        super(hc);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        SType type = methodPath.getType(stmt);
        if (methodPath.getJumpNode() == null)
            return type;

        hc.getSe().push(methodPath);

        JumpNode jn = methodPath.getJumpNode();
        Stmt jUnit = jn.getBlock().getStmts().get(jn.getIndex());
        if (type == SType.RETURN && jUnit instanceof JAssignStmt assignStmt) {
            JReturnStmt returnStmt = (JReturnStmt) stmt;
            Expr expr = hc.getZ3t().translateValue(
                    returnStmt.getOp(), assignStmt.getRightOp().getType(), VarType.RETURN_VALUE, methodPath);

            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            VarType varType = getVarType(methodPath, rightOp);

            Class classType = SootInterpreter.translateType(assignStmt.getRightOp().getType());
            hc.getZ3t().updateSymbolicVar(leftOp, expr, varType, jn.getMethodPath(), classType);
        }

        hc.getSe().analyzePaths(jn.getMethodPath(), jn.getBlock(), jn.getIndex());
        hc.getSe().pop(methodPath);

        return SType.INVOKE;
    }
}
