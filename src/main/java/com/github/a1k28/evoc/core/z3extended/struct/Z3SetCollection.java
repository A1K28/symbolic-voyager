package com.github.a1k28.evoc.core.z3extended.struct;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.*;

public class Z3SetCollection {
    private final Context ctx;
    private final Map<Integer, Set<Expr>> setMap;

    public Z3SetCollection(Context context) {
        this.ctx = context;
        this.setMap = new HashMap<>();
    }

    public void add(int hashCode, Expr o) {
        if (!setMap.containsKey(hashCode))
            setMap.put(hashCode, new HashSet<>());
        setMap.get(hashCode).add(o);
    }

    public BoolExpr contains(int hashCode, Expr element) {
        if (!setMap.containsKey(hashCode)) return ctx.mkBool(false);
        BoolExpr boolExpr = ctx.mkBool(false);
        for (Expr value : setMap.get(hashCode)) {
            boolExpr = ctx.mkOr(boolExpr, ctx.mkEq(value, element));
        }
        return boolExpr;
    }

    public int size(int hashCode) {
        return setMap.getOrDefault(hashCode, Collections.emptySet()).size();
    }
}
