package com.github.a1k28.symvoyager.core.z3extended.model;

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
    private IntExpr size;
    private IntExpr internalSize;
    private IntExpr capacity;
    private boolean isSizeUnknown;
    private List<Tuple<Function<IntExpr, IntExpr>>> indexMaps;

    public ListModel(Expr reference,
                     ArrayExpr array,
                     IntExpr size,
                     IntExpr capacity,
                     boolean isSizeUnknown) {
        this.reference = reference;
        this.array = array;
        this.size = size;
        this.internalSize = size;
        this.capacity = capacity;
        this.isSizeUnknown = isSizeUnknown;
        this.indexMaps = new ArrayList<>();
    }

    public ListModel(ListModel model) {
        this.reference = model.reference;
        this.array = model.array;
        this.size = model.size;
        this.internalSize = model.internalSize;
        this.capacity = model.capacity;
        this.isSizeUnknown = model.isSizeUnknown;
        this.indexMaps = new ArrayList<>(model.indexMaps);
    }
}
