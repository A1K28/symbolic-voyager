package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

// only 2 possibilies:
// 1. empty params list implies that all expressions should be interpreted.
// 2. full params list implies that all expressions are already set.
public class SParamList {
    private int index = 0;
    private final int size;
    private final List<Expr> expressions;
    private final List<Class> types;

    public SParamList() {
        this.size = 0;
        this.expressions = Collections.emptyList();
        this.types = Collections.emptyList();
    }

    public SParamList(List<Expr> expressions, List<Class> types) {
        this.expressions = expressions;
        this.types = types;
        this.size = expressions.size();
    }

    public boolean hasNext() {
        return index < size;
    }

    public Param getNext() {
        Param param = new Param(expressions.get(index), types.get(index));
        index++;
        return param;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Param {
        private final Expr expression;
        private final Class<?> type;
    }
}
