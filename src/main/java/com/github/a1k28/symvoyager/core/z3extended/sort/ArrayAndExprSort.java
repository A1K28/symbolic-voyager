package com.github.a1k28.symvoyager.core.z3extended.sort;

import com.microsoft.z3.*;
import lombok.Getter;

@Getter
public class ArrayAndExprSort extends AbstractSort {
    private final TupleSort sort;

    public ArrayAndExprSort(Context ctx, String name, Sort arraySort, Sort exprSort) {
        this.sort = ctx.mkTupleSort(
                ctx.mkSymbol(name),
                new Symbol[]{
                        ctx.mkSymbol("array"),
                        ctx.mkSymbol("expr")},
                new Sort[]{arraySort, exprSort}
        );
    }

    public Expr mkDecl(Expr array, Expr expr) {
        return this.sort.mkDecl().apply(array, expr);
    }

    public ArrayExpr getArray(Expr value) {
        return (ArrayExpr) this.sort.getFieldDecls()[0].apply(value);
    }

    public Expr getExpr(Expr value) {
        return this.sort.getFieldDecls()[1].apply(value);
    }
}
