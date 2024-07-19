package com.github.a1k28.evoc.core.executor.model;

import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

import java.util.List;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public class MethodModel {
    private final String name;
    private final BiFunction<AbstractInvokeExpr, List<Expr>, Expr> biFunction;
    private final boolean hasBase;

    public boolean hasBase() {
        return this.hasBase;
    }
}
