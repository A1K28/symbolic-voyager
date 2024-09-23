package com.github.a1k28.evoc.core.z3extended.sort;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.TupleSort;

public class ArrayAndExprSort extends AbstractSort {
    public ArrayAndExprSort(TupleSort sort) {
        super(sort);
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
