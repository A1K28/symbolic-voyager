package com.github.a1k28.evoc.core.z3extended.model;

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
    private Expr headReference;
    private Expr tailReference;
    private IntExpr size;
    private IntExpr capacity;
    private boolean isSizeUnknown;

    public LinkedListModel(Expr reference,
                           Expr headReference,
                           Expr tailReference,
                           IntExpr size,
                           IntExpr capacity,
                           boolean isSizeUnknown) {
        this.reference = reference;
        this.headReference = headReference;
        this.tailReference = tailReference;
        this.size = size;
        this.capacity = capacity;
        this.isSizeUnknown = isSizeUnknown;
    }

    public LinkedListModel(LinkedListModel model) {
        this.reference = model.reference;
        this.headReference = model.headReference;
        this.tailReference = model.tailReference;
        this.size = model.size;
        this.capacity = model.capacity;
        this.isSizeUnknown = model.isSizeUnknown;
    }
}
