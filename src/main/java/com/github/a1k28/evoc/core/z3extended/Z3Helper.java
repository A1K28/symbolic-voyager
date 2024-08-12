package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.SortContainer;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.microsoft.z3.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Z3Helper {
    private static final Map<String, SortContainer> listSorts = new HashMap<>();
    private static final Map<String, Map<String, SortContainer>> mapSorts = new HashMap<>();

    public static TupleSort mkMapSort(Context ctx, Sort key, Sort value) {
        return getMapSort(ctx, key, value).getSort();
    }

    public static Expr mkMapSentinel(Context ctx, Sort key, Sort value) {
        return getMapSort(ctx, key, value).getSentinel();
    }

    public static TupleSort mkListSort(Context ctx, Sort sort) {
        return getListSort(ctx, sort).getSort();
    }

    public static Expr mkListSentinel(Context ctx, Sort sort) {
        return getListSort(ctx, sort).getSentinel();
    }

    public static Sort getSort(Context ctx, Expr expr) {
        return getSort(ctx, List.of(expr));
    }

    public static Sort getSort(Context ctx, Expr[] expr) {
        return getSort(ctx, Arrays.stream(expr).collect(Collectors.toList()));
    }

    public static Sort getSort(Context ctx, List<Expr> exprs) {
        if (exprs == null || exprs.isEmpty())
            return SortType.OBJECT.value(ctx);
        Sort sort = exprs.get(0).getSort();
        if (sort == null || "Unknown".equalsIgnoreCase(sort.toString()))
            return SortType.OBJECT.value(ctx);
        for (Expr expr : exprs) {
            if (!sort.equals(expr.getSort()))
                SortType.OBJECT.value(ctx);
        }
        return sort;
    }

    public static ArithExpr increment(Context ctx, ArithExpr expr) {
        return ctx.mkAdd(expr, ctx.mkInt(1));
    }

    private static SortContainer getListSort(Context ctx, Sort sort) {
        String name = sort.getName().toString();
        if (!listSorts.containsKey(name)) {
            TupleSort tupleSort = ctx.mkTupleSort(
                    ctx.mkSymbol("listOptional:"+name),
                    new Symbol[]{
                            ctx.mkSymbol("value"),
                            ctx.mkSymbol("isEmpty")},
                    new Sort[]{sort, ctx.getBoolSort()}
            );
            Expr sentinel = tupleSort.mkDecl().apply(
                    ctx.mkConst("sentinel", sort), ctx.mkTrue());
            SortContainer container = new SortContainer(tupleSort, sentinel);
            listSorts.put(name, container);
        }
        return listSorts.get(name);
    }

    private static SortContainer getMapSort(Context ctx, Sort key, Sort value) {
        String keyName = key.getName().toString();
        String valueName = value.getName().toString();
        if (!mapSorts.containsKey(keyName)) mapSorts.put(keyName, new HashMap<>());
        if (!mapSorts.get(keyName).containsKey(valueName)) {
            TupleSort tupleSort = ctx.mkTupleSort(
                    ctx.mkSymbol("mapOptional:"+key+":"+value),
                    new Symbol[]{
                            ctx.mkSymbol("key"),
                            ctx.mkSymbol("value"),
                            ctx.mkSymbol("isEmpty")},
                    new Sort[]{key, value, ctx.getBoolSort()}
            );
            // TODO: fix this
            Expr sentinel = tupleSort.mkDecl().apply(
                    ctx.mkConst("sentinelKey", key), ctx.mkConst("sentinelValue", value), ctx.mkTrue());
//                    ctx.mkString("sentinelKey"), ctx.mkString("sentinelValue"), ctx.mkTrue());
            SortContainer container = new SortContainer(tupleSort, sentinel);
            mapSorts.get(keyName).put(valueName, container);
        }
        return mapSorts.get(keyName).get(valueName);
    }
}
