package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SParamList;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVar;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.Type;

public class ParameterHandler extends AbstractSymbolicHandler {
    public ParameterHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        if (methodPath.getParamList().hasNext()) { // is inner method
            updateParameter(hc.getZ3t(), methodPath, stmt);
        } else { // is target/first method
            saveParameter(hc.getZ3t(), hc.getCtx(), methodPath, stmt);
        }
        return SType.PARAMETER;
    }

    private void saveParameter(Z3Translator z3t, Z3ExtendedContext ctx, SMethodPath methodPath, Stmt unit) {
        Local ref = ((JIdentityStmt) unit).getLeftOp();
        Type type = ((JIdentityStmt) unit).getRightOp().getType();
        SVar param = z3t.saveSymbolicVar(
                ref,
                type,
                getVarType(methodPath, unit),
                methodPath);

        if (SortType.MAP.equals(param.getExpr().getSort()))
            ctx.getMapInstance().parameterConstructor(param.getExpr());
        else if (SortType.ARRAY.equals(param.getExpr().getSort()))
            ctx.getLinkedListInstance().parameterConstructor(param.getExpr());
        else if (SortType.OBJECT.equals(param.getExpr().getSort())
                && CLIOptions.shouldPropagate(param.getClassType().getName()))
            ctx.getClassInstance().parameterConstructor(param.getExpr(), param.getClassType());
    }

    private void updateParameter(Z3Translator z3t, SMethodPath methodPath, Stmt unit) {
        Local ref = ((JIdentityStmt) unit).getLeftOp();
        SParamList.Param param = methodPath.getParamList().getNext();
        z3t.updateSymbolicVar(ref,
                param.getExpression(),
                getVarType(methodPath, unit),
                methodPath);
    }
}
