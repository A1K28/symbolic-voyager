package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class MapModel {
    private Expr reference;
    private ArrayExpr array;
    private TupleSort sort;
    private ArithExpr size;
    private boolean isSizeUnknown;

    public MapModel(MapModel model) {
        this.reference = model.reference;
        this.array = model.array;
        this.size = model.size;
        this.isSizeUnknown = model.isSizeUnknown;
        this.sort = model.sort;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Entry {
        private Expr<TupleSort> key;
        private Expr<TupleSort> value;
    }

    // TODO: possibly refactor these
    public Expr mkDecl(Expr key, Expr element, BoolExpr isEmpty) {
        return this.sort.mkDecl().apply(key, element, isEmpty);
    }

    public Sort getKeySort() {
        return sort.getFieldDecls()[0].getRange();
    }

    public Sort getValueSort() {
        return sort.getFieldDecls()[0].getDomain()[0];
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
