package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class MapModel {
    private int hashCode;
    private ArrayExpr array;
    private TupleSort sort;
    private Expr sentinel;
    private List<Expr> discoveredKeys;
    private ArithExpr size;
    private boolean isSizeUnknown;
    private Integer lastCalcSizeHashCode;

    public MapModel(int hashCode,
                    ArrayExpr array,
                    ArithExpr size,
                    boolean isSizeUnknown,
                    TupleSort sort,
                    Expr sentinel) {
        this.hashCode = hashCode;
        this.array = array;
        this.size = size;
        this.sort = sort;
        this.sentinel = sentinel;
        this.discoveredKeys = new ArrayList<>();
        this.isSizeUnknown = isSizeUnknown;
    }

    public MapModel(MapModel model) {
        this.hashCode = model.hashCode;
        this.array = model.array;
        this.size = model.size;
        this.isSizeUnknown = model.isSizeUnknown;
        this.sort = model.sort;
        this.sentinel = model.sentinel;
        this.discoveredKeys = model.discoveredKeys;
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

    public void addDiscoveredKey(Expr key) {
        if (!this.discoveredKeys.contains(key))
            this.discoveredKeys.add(key);
    }

    public boolean isSizeOutdated() {
        int newHashCode = System.identityHashCode(array);
        boolean isOutdated = this.lastCalcSizeHashCode == null
                || this.lastCalcSizeHashCode != newHashCode;
        this.lastCalcSizeHashCode = newHashCode;
        return isOutdated;
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
