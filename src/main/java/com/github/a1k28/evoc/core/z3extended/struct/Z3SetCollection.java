//package com.github.a1k28.evoc.core.z3extended.struct;
//
//import com.github.a1k28.evoc.core.z3extended.model.IStack;
//import com.microsoft.z3.BoolExpr;
//import com.microsoft.z3.Context;
//import com.microsoft.z3.Expr;
//
//import java.util.*;
//
//public class Z3SetCollection implements IStack {
//    private final Context ctx;
//    private final Map<Integer, Set<Expr>> setMap;
//
//    @Override
//    public void push() {
//    }
//
//    @Override
//    public void pop() {
//    }
//
//    public Z3SetCollection(Context context) {
//        this.ctx = context;
//        this.setMap = new HashMap<>();
//    }
//
//    public void add(int hashCode, Expr element) {
//        if (!setMap.containsKey(hashCode))
//            setMap.put(hashCode, new HashSet<>());
//        setMap.get(hashCode).add(element);
//    }
//
//    public BoolExpr contains(int hashCode, Expr element) {
//        if (!setMap.containsKey(hashCode)) return ctx.mkBool(false);
//        Set<Expr> set = setMap.get(hashCode);
//        Expr[] expressions = new Expr[set.size()];
//        int i = 0;
//        for (Expr value : set) {
//            expressions[i] = ctx.mkEq(value, element);
//            i++;
//        }
//        return ctx.mkOr(expressions);
//    }
//
//    public void remove(int hashCode, Expr element) {
//        if (!setMap.containsKey(hashCode)) return;
//        setMap.get(hashCode).remove(element);
//    }
//
//    public int size(int hashCode) {
//        return setMap.getOrDefault(hashCode, Collections.emptySet()).size();
//    }
//}
