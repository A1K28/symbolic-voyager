package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ListModel {
    private ArrayExpr expr;
    private TupleSort sort;
    private Expr sentinel;
    private int size;
    private int capacity;

    public ListModel(ArrayExpr expr, TupleSort sort, Expr sentinel, Integer capacity) {
        this.expr = expr;
        this.sort = sort;
        this.sentinel = sentinel;
        this.size = 0;
        this.capacity = capacity == null ? Integer.MAX_VALUE : capacity;
    }

    public void incrementSize() {
        this.size++;
    }

    public Expr mkDecl(Expr element, BoolExpr isEmpty) {
        return this.sort.mkDecl().apply(element, isEmpty);
    }

    public Sort getSort() {
        return sort.getFieldDecls()[0].getRange();
    }

    public Expr getValue(Expr value) {
        return this.sort.getFieldDecls()[0].apply(value);
    }

    public BoolExpr isEmpty(Expr value) {
        return (BoolExpr) this.sort.getFieldDecls()[1].apply(value);
    }
}