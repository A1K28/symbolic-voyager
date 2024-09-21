package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
@Setter
@EqualsAndHashCode
public class ListModel {
    private Expr reference;
    private ArrayExpr array;
    private TupleSort sort;
    private Expr sentinel;
    private IntExpr size;
    private IntExpr internalSize;
    private IntExpr capacity;
    private boolean isSizeUnknown;
    private List<Function<IntExpr, IntExpr>> indexMaps;

    public ListModel(Expr reference,
                     ArrayExpr array,
                     TupleSort sort,
                     Expr sentinel,
                     IntExpr size,
                     IntExpr capacity,
                     boolean isSizeUnknown) {
        this.reference = reference;
        this.array = array;
        this.sort = sort;
        this.sentinel = sentinel;
        this.size = size;
        this.internalSize = size;
        this.capacity = capacity;
        this.isSizeUnknown = isSizeUnknown;
        this.indexMaps = new ArrayList<>();
    }

    public ListModel(ListModel model) {
        this.reference = model.reference;
        this.array = model.array;
        this.sort = model.sort;
        this.sentinel = model.sentinel;
        this.size = model.size;
        this.internalSize = model.internalSize;
        this.capacity = model.capacity;
        this.isSizeUnknown = model.isSizeUnknown;
        this.indexMaps = new ArrayList<>(model.indexMaps);
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
