package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.*;
import com.microsoft.z3.Expr;
import lombok.RequiredArgsConstructor;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.Stmt;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractSymbolicHandler {
    protected final SymbolicHandlerContext hc;

    abstract SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException;

    protected List<Expr> translateExpressions(SMethodExpr methodExpr, SMethodPath methodPath) {
        List<Value> args = methodExpr.getArgs();
        return args.stream()
                .map(e -> hc.getZ3t().translateValue(e, getVarType(methodPath, e), methodPath))
                .collect(Collectors.toList());
    }

    protected VarType getVarType(SMethodPath methodPath, Stmt unit) {
        String val = unit.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (methodPath.getClassInstance().getFieldNames().contains(val)) return VarType.FIELD;
        return VarType.getType(unit);
    }

    protected VarType getVarType(SMethodPath methodPath, Value value) {
        String val = value.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (methodPath.getClassInstance().getFieldNames().contains(val)) return VarType.FIELD;
        return VarType.getType(value);
    }

    protected void propagate(SMethodExpr methodExpr,
                             SMethodPath methodPath,
                             JumpNode jumpNode)
            throws ClassNotFoundException {
        SParamList paramList = createParamList(methodExpr, methodPath);
        Executable method = SootInterpreter.getMethod(methodExpr.getInvokeExpr());
        SClassInstance classInstance = getClassInstance(methodExpr.getBase(), methodPath, method);
        hc.getSe().analyzeSymbolicPaths(classInstance, method, paramList, jumpNode);
    }

    private SClassInstance getClassInstance(Value base,
                                            SMethodPath methodPath,
                                            Executable method)
            throws ClassNotFoundException {
        if (base != null && !hc.getZ3t().getValueName(base).equals("this")) {
            Expr expr = hc.getZ3t().translateValue(base, VarType.OTHER, methodPath);
            return hc.getCtx().getClassInstance().constructor(
                    expr, method.getDeclaringClass()).getClassInstance();
        }
        return hc.getSe().getClassInstance(method.getDeclaringClass());
    }

    private SParamList createParamList(SMethodExpr methodExpr, SMethodPath methodPath) {
        List<Value> args = methodExpr.getArgs();
        List<Expr> exprArgs = args.stream()
                .map(e -> hc.getZ3t().translateValue(e, getVarType(methodPath, e), methodPath))
                .collect(Collectors.toList());
        List<Class> types = args.stream()
                .map(e -> SootInterpreter.translateType(e.getType()))
                .collect(Collectors.toList());
        return new SParamList(exprArgs, types);
    }
}
