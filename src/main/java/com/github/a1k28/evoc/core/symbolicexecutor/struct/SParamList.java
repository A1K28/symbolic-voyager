package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.microsoft.z3.Expr;

import java.util.Collections;
import java.util.List;

// only 2 possibilies:
// 1. empty params list implies that all expressions should be interpreted.
// 2. full params list implies that all expressions are already set.
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
