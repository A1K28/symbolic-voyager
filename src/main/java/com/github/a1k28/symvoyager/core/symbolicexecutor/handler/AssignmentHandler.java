package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.MethodPropagationType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.*;
import com.microsoft.z3.Expr;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;

import java.lang.reflect.Method;
import java.util.List;

public class AssignmentHandler extends AbstractSymbolicHandler {
    public AssignmentHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        VarType leftOpVarType = getVarType(methodPath, leftOp);
        VarType rightOpVarType = getVarType(methodPath, rightOp);
        SExpr rightOpHolder = hc.getZ3t().translateAndWrapValue(rightOp, rightOpVarType, methodPath);

        if (rightOpHolder.getSType() == SType.INVOKE) {
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            if (methodExpr.getPropagationType() == MethodPropagationType.PROPAGATE) {
                JumpNode jumpNode = new JumpNode(methodPath, node);
                propagate(methodExpr, methodPath, jumpNode);
                return SType.INVOKE;
            } else if (methodExpr.getPropagationType() == MethodPropagationType.MODELLED) {
                Class<?> type = SootInterpreter.translateType(rightOp.getType());
                Expr expr = hc.getZ3t().callProverMethod(rightOpHolder.asMethod(), rightOpVarType, methodPath);
                // TODO: handle complex nested objects within parameters
//                expr = ctx.mkDefault(expr, Z3Translator.translateType(rightOp.getType()));
                hc.getZ3t().updateSymbolicVar(leftOp, expr, leftOpVarType, methodPath, type);
            }
        } else if (rightOpHolder.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
            Class<?> type = SootInterpreter.translateType(rightOp.getType());
            Expr leftOpExpr = hc.getZ3t().translateValue(leftOp, leftOpVarType, methodPath);
            Expr expr = hc.getCtx().getClassInstance().constructor(
                    leftOpExpr, type).getExpr();
            hc.getCtx().getClassInstance().initialize(expr, type);
            hc.getZ3t().updateSymbolicVar(leftOp, expr, leftOpVarType, methodPath, type);
        } else if (rightOpHolder.getSType() == SType.INVOKE_MOCK) {
            // if method cannot be invoked, then mock it.
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            Class rightClassType = SootInterpreter.translateType(rightOp.getType());
            List<Expr> params = translateExpressions(methodExpr, methodPath);
            Method method = (Method) SootInterpreter.getMethod(methodExpr.getInvokeExpr());
            Expr retValExpr = hc.getZ3t().translateValue(rightOp, rightOpVarType, methodPath);
            Expr reference = hc.getCtx().getMethodMockInstance().constructor(
                    method, params, null, retValExpr).getReferenceExpr();
            SVar mockVar = new SVar(hc.getZ3t().getValueName(leftOp), reference,
                    VarType.METHOD_MOCK, rightClassType, true);
            methodPath.addMethodMock(mockVar);
            return SType.INVOKE_MOCK;
        } else if (rightOp instanceof JCastExpr castExpr) {
            Value op = castExpr.getOp();
            Class classType = methodPath.getSymbolicVarStack().get(hc.getZ3t().getValueName(op)).
                    orElseThrow().getClassType();
            hc.getZ3t().updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, methodPath, classType);
        } else {
            Class classType = SootInterpreter.translateType(rightOp.getType());
            if (classType == Object.class) classType = SootInterpreter.translateType(leftOp.getType());
            hc.getZ3t().updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, methodPath, classType);
        }
        return SType.ASSIGNMENT;
    }
}
