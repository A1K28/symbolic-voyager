package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.SortContainer;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.microsoft.z3.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Z3CachingFactory {
    private final Context ctx;
    private final Map<String, SortContainer> listSorts = new HashMap<>();
    private final Map<String, Map<String, SortContainer>> linkedListSorts = new HashMap<>();
    private final Map<String, Map<String, SortContainer>> mapSorts = new HashMap<>();

    public Z3CachingFactory(Context ctx) {
        this.ctx = ctx;
    }

    public TupleSort mkMapSort(Sort key, Sort value) {
        return getMapSort(key, value).getSort();
    }

    public Expr mkMapSentinel(Sort key, Sort value) {
        return getMapSort(key, value).getSentinel();
    }

    public TupleSort mkListSort(Sort sort) {
        return getListSort(sort).getSort();
    }

    public Expr mkListSentinel(Sort sort) {
        return getListSort(sort).getSentinel();
    }

    public TupleSort mkLinkedListSort(Sort keySort, Sort valueSort) {
        return getLinkedListSort(keySort, valueSort).getSort();
    }

    public Sort getSort(Expr expr) {
        return getSort(List.of(expr));
    }

    public Sort getSort(Expr[] expr) {
        return getSort(Arrays.stream(expr).collect(Collectors.toList()));
    }

    public Sort getSort(List<Expr> exprs) {
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

    private SortContainer getListSort(Sort sort) {
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

    private SortContainer getLinkedListSort(Sort keySort, Sort valueSort) {
        String keyName = keySort.getName().toString();
        String valueName = valueSort.getName().toString();
        if (!linkedListSorts.containsKey(keyName)) linkedListSorts.put(keyName, new HashMap<>());
        if (!linkedListSorts.get(keyName).containsKey(valueName)) {
            TupleSort tupleSort = ctx.mkTupleSort(
                    ctx.mkSymbol("linkedList:"+keyName+":"+valueName),
                    new Symbol[]{
                            ctx.mkSymbol("value"),
                            ctx.mkSymbol("ref"),
                            ctx.mkSymbol("nextRef"),
                            ctx.mkSymbol("prevRef")},
                    new Sort[]{valueSort, keySort, keySort, keySort}
            );
            SortContainer container = new SortContainer(tupleSort, null);
            linkedListSorts.get(keyName).put(valueName, container);
        }
        return linkedListSorts.get(keyName).get(valueName);
    }

    private SortContainer getMapSort(Sort key, Sort value) {
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
            Expr sentinel = tupleSort.mkDecl().apply(
                    ctx.mkConst("sentinelKey", key),
                    ctx.mkConst("sentinelValue", value),
                    ctx.mkTrue());
            SortContainer container = new SortContainer(tupleSort, sentinel);
            mapSorts.get(keyName).put(valueName, container);
        }
        return mapSorts.get(keyName).get(valueName);
    }
}
