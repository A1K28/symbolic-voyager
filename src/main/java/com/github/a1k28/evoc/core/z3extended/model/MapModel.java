package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class MapModel {
    private int hashCode;
    private ArrayExpr array;
    private TupleSort sort;
    private Expr sentinel;
    private KeyExprs discoveredKeys;
    private ArrayExpr addedKeySet; // a set of keys that were added to the array
    private ArithExpr size;
    private boolean isSizeUnknown;

    public MapModel(int hashCode,
                    ArrayExpr array,
                    ArithExpr size,
                    boolean isSizeUnknown,
                    ArrayExpr addedKeySet,
                    TupleSort sort,
                    Expr sentinel) {
        this.hashCode = hashCode;
        this.array = array;
        this.size = size;
        this.sort = sort;
        this.sentinel = sentinel;
        this.addedKeySet = addedKeySet;
        this.discoveredKeys = new KeyExprs();
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
        this.addedKeySet = model.addedKeySet;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Entry {
        private Expr<TupleSort> key;
        private Expr<TupleSort> value;
    }

    public Expr mkDecl(Expr key, Expr element, BoolExpr isEmpty, BoolExpr wasInitiallyPresent) {
        return this.sort.mkDecl().apply(key, element, isEmpty);
    }

    public void addDiscoveredKey(Expr key) {
        this.discoveredKeys.add(key, null);
    }

    public void addDiscoveredKey(Expr key, BoolExpr wasInitiallyPresent) {
        this.discoveredKeys.add(key, wasInitiallyPresent);
    }

    public List<Expr> getDiscoveredKeys() {
        return this.discoveredKeys.getKeys();
    }

    public KeyExprs getKeyExprs() {
        return this.discoveredKeys;
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
