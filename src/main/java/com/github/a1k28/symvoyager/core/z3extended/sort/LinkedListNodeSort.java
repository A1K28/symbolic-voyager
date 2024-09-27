package com.github.a1k28.symvoyager.core.z3extended.sort;

import com.microsoft.z3.*;
import lombok.Getter;

@Getter
public class LinkedListNodeSort extends AbstractSort {
    private final TupleSort sort;

    public LinkedListNodeSort(Context ctx,
                              String name,
                              Sort referenceSort,
                              Sort valueSort) {
        this.sort = ctx.mkTupleSort(
                ctx.mkSymbol(name),
                new Symbol[]{
                        ctx.mkSymbol("value"),
                        ctx.mkSymbol("ref"),
                        ctx.mkSymbol("nextRef"),
                        ctx.mkSymbol("prevRef")},
                new Sort[]{valueSort, referenceSort, referenceSort, referenceSort}
        );
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
