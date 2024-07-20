package com.github.a1k28.evoc.core.z3extended.struct;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.*;

public class Z3ListCollection {
    private final Context ctx;
    private final Map<Integer, List<Expr>> listMap;

    public Z3ListCollection(Context context) {
        this.ctx = context;
        this.listMap = new HashMap<>();
    }

    private Expr sizeExpr(int hashCode) {
        return ctx.mkInt(listMap.getOrDefault(hashCode, Collections.emptyList()).size());
    }

    public BoolExpr isEmpty(int hashCode) {
        return ctx.mkBool(size(hashCode) == 0);
    }

    public BoolExpr add(int hashCode, Expr element) {
        if (!listMap.containsKey(hashCode))
            listMap.put(hashCode, new ArrayList<>());
        int size = size(hashCode);
        listMap.get(hashCode).add(element);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr add(int hashCode, int index, Expr element) {
        if (!listMap.containsKey(hashCode))
            listMap.put(hashCode, new ArrayList<>());
        List<Expr> list = listMap.get(hashCode);
        int size = list.size();
        if (index > size)
            index = size;
        list.add(index, element);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr addAll(int hashCode, List<Expr> elements) {
        int size = size(hashCode);
        for (Expr expr : elements) add(hashCode, expr);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr addAll(int hashCode, int index, List<Expr> elements) {
        int size = size(hashCode);
        for (int idx = index, i = 0; idx < index+elements.size(); idx++, i++)
            add(hashCode, idx, elements.get(i));
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr remove(int hashCode, Expr element) {
        if (!listMap.containsKey(hashCode)) return ctx.mkBool(false);
        int size = size(hashCode);
        listMap.get(hashCode).remove(element);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr remove(int hashCode, int index) {
        if (!listMap.containsKey(hashCode)) return ctx.mkBool(false);
        int size = size(hashCode);
        listMap.get(hashCode).remove(index);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr removeAll(int hashCode, List<Expr> element) {
        int size = size(hashCode);
        for (Expr expr : element) remove(hashCode, expr);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr contains(int hashCode, Expr element) {
        if (!listMap.containsKey(hashCode)) return ctx.mkBool(false);
        List<Expr> set = listMap.get(hashCode);
        Expr[] expressions = new Expr[set.size()];
        int i = 0;
        for (Expr value : set) {
            expressions[i] = ctx.mkEq(value, element);
            i++;
        }
        return ctx.mkOr(expressions);
    }

    public BoolExpr containsAll(int hashCode, List<Expr> elements) {
        if (!listMap.containsKey(hashCode)) return ctx.mkBool(false);
        List<Expr> set = listMap.get(hashCode);
        Expr[] expressions = new Expr[elements.size()];
        int i = 0;
        for (Expr element : elements) {
            Expr[] exps = new Expr[set.size()];
            int j = 0;
            for (Expr se : exps) {
                exps[j] = ctx.mkEq(se, element);
                j++;
            }
            expressions[i] = ctx.mkOr(exps);
            i++;
        }
        return ctx.mkAnd(expressions);
    }

    public BoolExpr retainAll(int hashCode, List<Expr> elements) {
        int size = size(hashCode);
        for (Expr expr : get(hashCode))
            if (!elements.contains(expr))
                remove(hashCode, expr);
        int newSize = size(hashCode);
        return ctx.mkBool(newSize != size);
    }

    public void clear(int hashCode) {
        get(hashCode).clear();
    }

    public BoolExpr equals(int hashCode, ArrayExpr list) {
        if (!listMap.containsKey(hashCode)) return ctx.mkBool(false);
        List<Expr> elements = get(hashCode);
        Expr[] exprs = list.getArgs();
        if (elements.size() != exprs.length) return ctx.mkBool(false);
        Expr[] result = new Expr[elements.size()];
        for (int i = 0; i < elements.size(); i++)
            result[i] = ctx.mkEq(elements.get(i), exprs[i]);
        return ctx.mkAnd(result);
    }

    public Expr get(int hashCode, int index) {
        List<Expr> elements = get(hashCode);
        if (elements.size() >= index) return elements.get(elements.size()-1);
        return elements.get(index);
    }

    public Expr set(int hashCode, int index, Expr element) {
        if (listMap.containsKey(hashCode)) {
            List<Expr> elements = get(hashCode);
            if (elements.size() >= index) index = elements.size();
            listMap.get(hashCode).add(index, element);
        }
        return element;
    }

    public Expr indexOf(int hashCode, Expr element) {
        return ctx.mkInt(get(hashCode).indexOf(element));
    }

    public Expr lastIndexOf(int hashCode, Expr element) {
        return ctx.mkInt(get(hashCode).lastIndexOf(element));
    }

    private int size(int hashCode) {
        return get(hashCode).size();
    }

    private List<Expr> get(int hashCode) {
        return listMap.getOrDefault(hashCode, Collections.emptyList());
    }
}
