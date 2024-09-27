package com.github.a1k28.symvoyager.core.z3extended.model;

import com.microsoft.z3.Expr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MethodMockExprModel {
    private final Expr referenceExpr;
    private final Method method;
    private final List<Expr> args;
    private ClassType exceptionType;
    private final Expr retVal;

    public boolean throwsException() {
        return exceptionType != null;
    }
}
