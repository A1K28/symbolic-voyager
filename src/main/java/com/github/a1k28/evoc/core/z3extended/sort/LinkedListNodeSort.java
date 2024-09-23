package com.github.a1k28.evoc.core.z3extended.sort;

import com.microsoft.z3.Expr;
import com.microsoft.z3.TupleSort;

public class LinkedListNodeSort extends AbstractSort {
    public LinkedListNodeSort(TupleSort sort) {
        super(sort);
    }

    public Expr mkDecl(Expr element, Expr ref, Expr nextRef, Expr prevRef) {
        return this.sort.mkDecl().apply(element, ref, nextRef, prevRef);
    }

    public Expr getValue(Expr node) {
        return this.sort.getFieldDecls()[0].apply(node);
    }

    public Expr getRef(Expr node) {
        return this.sort.getFieldDecls()[1].apply(node);
    }

    public Expr getNextRef(Expr node) {
        return this.sort.getFieldDecls()[2].apply(node);
    }

    public Expr getPrevRef(Expr node) {
        return this.sort.getFieldDecls()[3].apply(node);
    }
}
