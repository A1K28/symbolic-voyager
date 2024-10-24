package com.github.a1k28.symvoyager.core.z3extended.model;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class LinkedListModel {
    private Expr reference;
    private ArrayExpr referenceMap;
    private Expr headReference;
    private Expr tailReference;
    private IntExpr size;
    private IntExpr refCounter;

    public LinkedListModel(Expr reference,
                           ArrayExpr referenceMap,
                           Expr headReference,
                           Expr tailReference,
                           IntExpr size,
                           IntExpr refCounter) {
        this.reference = reference;
        this.referenceMap = referenceMap;
        this.headReference = headReference;
        this.tailReference = tailReference;
        this.size = size;
        this.refCounter = refCounter;
    }

    public LinkedListModel(LinkedListModel model) {
        this.reference = model.reference;
        this.referenceMap = model.referenceMap;
        this.headReference = model.headReference;
        this.tailReference = model.tailReference;
        this.size = model.size;
        this.refCounter = model.refCounter;
    }
}
