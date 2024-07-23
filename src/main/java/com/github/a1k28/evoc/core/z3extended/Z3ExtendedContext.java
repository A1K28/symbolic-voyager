package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.struct.Z3ListCollection;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SetCollection;
import com.microsoft.z3.*;

import java.util.List;

public class Z3ExtendedContext extends Context {
    private final Z3SetCollection z3SetCollection;
    private final Z3ListCollection z3ListCollection;

    public Z3ExtendedContext() {
        super();
        this.z3SetCollection = new Z3SetCollection(this);
        this.z3ListCollection = new Z3ListCollection(this);
    }

    // sets
    @Override
    public <D extends Sort> ArrayExpr<D, BoolSort> mkSetAdd(Expr<ArraySort<D, BoolSort>> var1, Expr<D> var2) {
        z3SetCollection.add(System.identityHashCode(var1), var2);
        return super.mkSetAdd(var1, var2);
    }

    public Expr mkList(Expr var1) {
        return z3ListCollection.constructor(var1, null, null);
    }

    public Expr mkListWithCapacity(Expr var1, IntExpr capacity) {
        return z3ListCollection.constructor(var1, parseInt(capacity), null);
    }

    public Expr mkListWithCollection(Expr var1, Expr var2) {
        return z3ListCollection.constructor(var1, var2);
    }

    public Expr mkListWithCollection(List<Expr> vars) {
        return z3ListCollection.constructor(getSort(vars), vars.toArray(new Expr[0]));
    }

    public Expr mkListLength(Expr var1) {
        return z3ListCollection.sizeExpr(var1);
    }

    public BoolExpr mkListIsEmpty(Expr var1) {
        return z3ListCollection.isEmpty(var1);
    }

    public BoolExpr mkListAdd(Expr var1, Expr element) {
        return z3ListCollection.add(var1, element);
    }

    public BoolExpr mkListAdd(Expr var1, IntExpr index, Expr var2) {
        return z3ListCollection.add(var1, parseInt(index), var2);
    }

    public BoolExpr mkListAddAll(Expr var1, Expr var2) {
        return z3ListCollection.addAll(var1, var2);
    }

    public BoolExpr mkListAddAll(Expr var1, IntExpr index, Expr var2) {
        return z3ListCollection.addAll(var1, parseInt(index), var2);
    }

    public BoolExpr mkListRemove(Expr var1, Expr element) {
        return z3ListCollection.remove(var1, element);
    }

    public BoolExpr mkListRemove(Expr var1, IntExpr index) {
        return z3ListCollection.remove(var1, parseInt(index));
    }

    public BoolExpr mkListRemoveAll(Expr var1, Expr element) {
        return z3ListCollection.removeAll(var1, null);
    }

    public BoolExpr mkListContains(Expr var1, Expr element) {
        return z3ListCollection.contains(var1, element);
    }

    public BoolExpr mkListContainsAll(Expr var1, Expr elements) {
        return z3ListCollection.containsAll(var1, null);
    }

    public BoolExpr mkListRetainAll(Expr var1, Expr elements) {
        return z3ListCollection.retainAll(var1, null);
    }

    public Expr mkListClear(Expr var1) {
        return z3ListCollection.clear(var1);
    }

    public BoolExpr mkListEquals(Expr var1, Expr expr) {
        return z3ListCollection.equals(var1, null);
    }

    public Expr mkListGet(Expr var1, IntExpr index) {
        return z3ListCollection.get(var1, parseInt(index));
    }

    public Expr mkListSet(Expr var1, IntExpr index, Expr element) {
        return z3ListCollection.set(var1, parseInt(index), element);
    }

    public Expr mkListIndexOf(Expr var1, Expr element) {
        return z3ListCollection.indexOf(var1, element);
    }

    public Expr mkListLastIndexOf(Expr var1, Expr element) {
        return z3ListCollection.lastIndexOf(var1, element);
    }

    public Expr mkSetLength(Expr expr) {
        return super.mkInt(z3SetCollection.size(System.identityHashCode(expr)));
    }

    public Expr mkSetContains(Expr var1, Expr var2) {
        return z3SetCollection.contains(System.identityHashCode(var1), var2);
    }

    public Expr mkSetRemove(Expr var1, Expr var2) {
        z3SetCollection.remove(System.identityHashCode(var1), var2);
        return super.mkSetDel(var1, var2);
    }

    private int parseInt(IntExpr expr) {
        return Integer.parseInt(expr.toString());
    }

    private Sort getSort(List<Expr> exprs) {
        return getSort(this, exprs);
    }

    public static Sort getSort(Context ctx, List<Expr> exprs) {
        if (exprs == null || exprs.isEmpty())
            return ctx.mkUninterpretedSort("Object");
        Sort sort = exprs.get(0).getSort();
        if (sort == null || "Unknown".equalsIgnoreCase(sort.toString()))
            return ctx.mkUninterpretedSort("Object");
        for (Expr expr : exprs) {
            if (!sort.equals(expr.getSort()))
                ctx.mkUninterpretedSort("Object");
        }
        return sort;
    }
}
