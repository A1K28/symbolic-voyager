package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
import com.microsoft.z3.Expr;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;

import java.util.List;

public class ReturnHandler extends AbstractSymbolicHandler {
    public ReturnHandler(SymbolicHandlerContext hc) {
        super(hc);
    }

    @Override
    public SType handle(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        if (methodPath.getJumpNode() == null)
            return node.getType();

        hc.getSe().push(methodPath);

        JumpNode jn = methodPath.getJumpNode();
        if (node.getType() == SType.RETURN
                && jn.getNode().getUnit() instanceof JAssignStmt assignStmt) {
            JReturnStmt stmt = (JReturnStmt) node.getUnit();
            Expr expr = hc.getZ3t().translateValue(
                    stmt.getOp(), assignStmt.getRightOp().getType(), VarType.RETURN_VALUE, methodPath);

            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            VarType varType = getVarType(methodPath, rightOp);

            Class classType = SootInterpreter.translateType(assignStmt.getRightOp().getType());
            hc.getZ3t().updateSymbolicVar(leftOp, expr, varType, jn.getMethodPath(), classType);
        }

        List<SNode> children = jn.getNode().getChildren();
        for (SNode child : children)
            hc.getSe().analyzePaths(jn.getMethodPath(), child);

        hc.getSe().pop(methodPath);

        return SType.INVOKE;
    }
}
