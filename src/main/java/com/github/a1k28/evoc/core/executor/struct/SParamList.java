package com.github.a1k28.evoc.core.executor.struct;

import com.microsoft.z3.Expr;

import java.util.Collections;
import java.util.List;

public class SParamList {
    private int index = 0;
    private final int size;
    private final List<Expr> expressions;

    public SParamList() {
        this.size = 0;
        this.expressions = Collections.emptyList();
    }

    public SParamList(List<Expr> expressions) {
        this.expressions = expressions;
        this.size = expressions.size();
    }

    public boolean hasNext() {
        return index < size;
    }

    public Expr getNext() {
        return expressions.get(index++);
    }
}
