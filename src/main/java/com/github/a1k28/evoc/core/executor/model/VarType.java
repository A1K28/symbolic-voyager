package com.github.a1k28.evoc.core.executor.model;

import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;

public enum VarType {
    FIELD,
    PARAMETER,
    LOCAL,
    RETURN_VALUE,
    METHOD,
    METHOD_MOCK,
    BASE_ARG,
    METHOD_ARG,
    OTHER;

    public static VarType getType(Stmt unit) {
        if (unit instanceof JIdentityStmt u) {
            Value val = u.getRightOp();
            if (val instanceof JParameterRef) {
                return PARAMETER;
            }
            return LOCAL;
        }
        return OTHER;
    }

    public static VarType getType(Value unit) {
        if (unit instanceof AbstractInstanceInvokeExpr)
            return METHOD;
        return OTHER;
    }
}