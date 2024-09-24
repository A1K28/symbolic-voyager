package com.github.a1k28.evoc.core.z3extended.sort;

import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.microsoft.z3.*;
import lombok.Getter;

@Getter
public class LinkedListSort extends AbstractSort {
    private final TupleSort sort;

    public LinkedListSort(Context ctx,
                          String name,
                          Sort referenceSort,
                          ArraySort referenceMapSort) {
        this.sort = ctx.mkTupleSort(
                ctx.mkSymbol(name),
                new Symbol[]{
                        ctx.mkSymbol("reference"),
                        ctx.mkSymbol("referenceMap"),
                        ctx.mkSymbol("headReference"),
                        ctx.mkSymbol("tailReference"),
                        ctx.mkSymbol("size"),
                        ctx.mkSymbol("refCounter")},
                new Sort[]{SortType.ARRAY.value(ctx),
                        referenceMapSort,
                        referenceSort,
                        referenceSort,
                        ctx.mkIntSort(),
                        ctx.mkIntSort()}
        );
    }

    public Expr mkDecl(Expr reference,
                       ArrayExpr referenceMap,
                       Expr headReference,
                       Expr tailReference,
                       IntExpr size,
                       IntExpr refCounter) {
        return this.sort.mkDecl().apply(
                reference, referenceMap, headReference, tailReference, size, refCounter);
    }

    public Expr getReference(Expr value) {
        return this.sort.getFieldDecls()[0].apply(value);
    }

    public ArrayExpr getReferenceMap(Expr value) {
        return (ArrayExpr) this.sort.getFieldDecls()[1].apply(value);
    }

    public Expr getHeadReference(Expr value) {
        return this.sort.getFieldDecls()[2].apply(value);
    }

    public Expr getTailReference(Expr value) {
        return this.sort.getFieldDecls()[3].apply(value);
    }

    public IntExpr getSize(Expr value) {
        return (IntExpr) this.sort.getFieldDecls()[4].apply(value);
    }

    public IntExpr getRefCounter(Expr value) {
        return (IntExpr) this.sort.getFieldDecls()[5].apply(value);
    }
}
