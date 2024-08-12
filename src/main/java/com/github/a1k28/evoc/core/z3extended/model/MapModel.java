package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class MapModel {
    private ArrayExpr array;
    private ArithExpr size;
    private TupleSort sort;
    private Expr sentinel;

    public MapModel(ArrayExpr array,
                    ArithExpr size,
                    TupleSort sort,
                    Expr sentinel) {
        this.array = array;
        this.size = size;
        this.sort = sort;
        this.sentinel = sentinel;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Entry {
        private Expr<TupleSort> key;
        private Expr<TupleSort> value;
    }

    public Expr mkDecl(Expr key, Expr element, BoolExpr isEmpty) {
        return this.sort.mkDecl().apply(key, element, isEmpty);
    }

    public Sort getSort() {
        return sort.getFieldDecls()[0].getRange();
    }

    public Expr getKey(Expr value) {
        return this.sort.getFieldDecls()[0].apply(value);
    }

    public Expr getValue(Expr value) {
        return this.sort.getFieldDecls()[1].apply(value);
    }

    public BoolExpr isEmpty(Expr value) {
        return (BoolExpr) this.sort.getFieldDecls()[2].apply(value);
    }
}
