package com.github.a1k28.symvoyager.core.symbolicexecutor.handler.impl;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.AbstractSymbolicHandler;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.MethodPropagationType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.*;
import com.microsoft.z3.Expr;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.List;

public class VoidMethodCallHandler extends AbstractSymbolicHandler {
    public VoidMethodCallHandler(SymbolicHandlerContext hc) {
        super(hc);
    }

    @Override
    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        JInvokeStmt invoke = (JInvokeStmt) stmt;
        SExpr wrapped = hc.getZ3t().wrapMethodCall(invoke.getInvokeExpr(), null);
        VarType varType = getVarType(methodPath, invoke);

        if (wrapped.getSType() == SType.INVOKE
                || wrapped.getSType() == SType.INVOKE_MOCK
                || wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
            SMethodExpr method = wrapped.asMethod();

            // handle modelled
            if (method.getPropagationType() == MethodPropagationType.MODELLED) {
                hc.getZ3t().callProverMethod(method, varType, methodPath);
                return SType.MODELLED;
            }

            // handle mocks
            if (wrapped.getSType() == SType.INVOKE_MOCK) {
                String name = stmt.toString();
                List<Expr> params = translateExpressions(method, methodPath);
                Method javaMethod = (Method) SootInterpreter.getMethod(method.getInvokeExpr());
                Expr reference = hc.getCtx().getMethodMockInstance().constructor(
                        javaMethod, params, null, null).getReferenceExpr();
                SVar var = new SVar(name, reference,
                        VarType.METHOD_MOCK, null, true);
                methodPath.addMethodMock(var);
                hc.handle(methodPath, stmt, SType.INVOKE_MOCK);
                return SType.INVOKE_MOCK;
            }

            // set all values to default
            if (wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
                ClassType classType = method.getInvokeExpr().getMethodSignature().getDeclClassType();
                Class<?> type = SootInterpreter.translateType(classType);
                Expr leftOpExpr = hc.getZ3t().translateValue(method.getBase(), varType, methodPath);
                Expr expr = hc.getCtx().getClassInstance().constructor(
                        leftOpExpr, SootInterpreter.getClass(classType)).getExpr();
                hc.getCtx().getClassInstance().initialize(expr, type);
            }

            JumpNode jumpNode = methodPath.createJumpNode(stmt);
            propagate(method, methodPath, jumpNode);
            return SType.INVOKE;
        }

        return wrapped.getSType();
    }
}
