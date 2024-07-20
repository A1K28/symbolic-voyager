package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.struct.Z3SetCollection;
import com.microsoft.z3.*;

class Z3ExtendedContext extends Context {
    private final Z3SetCollection z3SetCollection;

    public Z3ExtendedContext() {
        super();
        this.z3SetCollection = new Z3SetCollection(this);
    }

    @Override
    public <D extends Sort> ArrayExpr<D, BoolSort> mkSetAdd(Expr<ArraySort<D, BoolSort>> var1, Expr<D> var2) {
        z3SetCollection.add(System.identityHashCode(var1), var2);
        return super.mkSetAdd(var1, var2);
    }

    Expr mkSetLength(Expr expr) {
        return super.mkInt(z3SetCollection.size(System.identityHashCode(expr)));
    }

    Expr mkSetContains(Expr var1, Expr var2) {
        return z3SetCollection.contains(System.identityHashCode(var1), var2);
    }

    Expr mkSetRemove(Expr var1, Expr var2) {
        z3SetCollection.remove(System.identityHashCode(var1), var2);
        return super.mkSetDel(var1, var2);
    }
}
