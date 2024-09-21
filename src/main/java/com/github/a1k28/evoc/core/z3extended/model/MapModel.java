package com.github.a1k28.evoc.core.z3extended.model;

import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class MapModel {
    private Expr reference;
    private final Z3SortUnion sortUnion;
    private ArrayExpr array;
    private TupleSort sort;
    private Expr sentinel;
    private ArithExpr size;
    private boolean isSizeUnknown;
    private Integer lastCalcSizeHashCode;

    public MapModel(Expr reference,
                    Z3SortUnion sortUnion,
                    ArrayExpr array,
                    ArithExpr size,
                    boolean isSizeUnknown,
                    TupleSort sort,
                    Expr sentinel) {
        this.reference = reference;
        this.sortUnion = sortUnion;
        this.array = array;
        this.size = size;
        this.sort = sort;
        this.sentinel = sentinel;
        this.isSizeUnknown = isSizeUnknown;
    }

    public MapModel(MapModel model) {
        this.reference = model.reference;
        this.sortUnion = model.sortUnion;
        this.array = model.array;
        this.size = model.size;
        this.isSizeUnknown = model.isSizeUnknown;
        this.sort = model.sort;
        this.sentinel = model.sentinel;
        this.lastCalcSizeHashCode = model.lastCalcSizeHashCode;
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
