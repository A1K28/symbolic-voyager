package com.github.a1k28.symvoyager.core.z3extended.sort;

import com.microsoft.z3.*;
import lombok.Getter;

@Getter
public class MapSort extends AbstractSort {
    private final TupleSort sort;
    private final Expr sentinel;

    public MapSort(Context ctx, String name, Sort key, Sort value) {
        this.sort = ctx.mkTupleSort(
                ctx.mkSymbol(name),
                new Symbol[]{
                        ctx.mkSymbol("key"),
                        ctx.mkSymbol("value"),
                        ctx.mkSymbol("isEmpty")},
                new Sort[]{key, value, ctx.getBoolSort()}
        );
        this.sentinel = sort.mkDecl().apply(
                ctx.mkConst("sentinelKey", key),
                ctx.mkConst("sentinelValue", value),
                ctx.mkTrue());
    }

    public Expr mkDecl(Expr key, Expr element, BoolExpr isEmpty) {
        return this.sort.mkDecl().apply(key, element, isEmpty);
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
