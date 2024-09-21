package com.github.a1k28.evoc.core.z3extended.model;

import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
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
    private final Z3SortUnion sortUnion;
    private ArrayExpr array;
    private TupleSort sort;
    private Expr sentinel;
    private ArithExpr size;
    private boolean isSizeUnknown;
    private Integer lastCalcSizeHashCode;
    private List<Tuple<Expr>> discoveredKeys; // (unwrapped, wrapped)


    public MapModel(int hashCode,
                    Z3SortUnion sortUnion,
                    ArrayExpr array,
                    ArithExpr size,
                    boolean isSizeUnknown,
                    TupleSort sort,
                    Expr sentinel,
                    List<Tuple<Expr>> discoveredKeys) {
        this.hashCode = hashCode;
        this.sortUnion = sortUnion;
        this.array = array;
        this.size = size;
        this.sort = sort;
        this.sentinel = sentinel;
        this.discoveredKeys = discoveredKeys;
        this.isSizeUnknown = isSizeUnknown;
    }

    public MapModel(MapModel model) {
        this.hashCode = model.hashCode;
        this.sortUnion = model.sortUnion;
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

    public List<Tuple<Expr>> getDiscoveredKeys() {
        return discoveredKeys;
    }

    public void addDiscoveredKey(Expr key) {
        if (!containsDiscoveredKey(key)) {
            Tuple<Expr> tuple = new Tuple<>(key, sortUnion.wrapValue(key));
            this.discoveredKeys.add(tuple);
        }
    }

    public void addUndiscoveredKey(Expr keyWrapped) {
        if (!containsWrappedDiscoveredKey(keyWrapped)) {
            Tuple<Expr> tuple = new Tuple<>(null, keyWrapped);
            this.discoveredKeys.add(tuple);
        }
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

    private boolean containsDiscoveredKey(Expr keyUnwrapped) {
        for (Tuple<Expr> tuple : discoveredKeys) {
            if (keyUnwrapped.equals(tuple.getO1())) return true;
        }
        return false;
    }

    private boolean containsWrappedDiscoveredKey(Expr keyWrapped) {
        for (Tuple<Expr> tuple : discoveredKeys) {
            if (keyWrapped.equals(tuple.getO2())) return true;
        }
        return false;
    }
}
