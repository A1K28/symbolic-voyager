package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ListModel {
    private ArrayExpr expr;
    private Sort sort;
    private Expr sentinel;
    private int size;
    private int capacity;

    public ListModel(ArrayExpr expr, Sort sort, Expr sentinel, Integer capacity) {
        this.expr = expr;
        this.sort = sort;
        this.sentinel = sentinel;
        this.size = 0;
        this.capacity = capacity == null ? Integer.MAX_VALUE : capacity;
    }

    public void incrementSize() {
        this.size++;
    }
}
