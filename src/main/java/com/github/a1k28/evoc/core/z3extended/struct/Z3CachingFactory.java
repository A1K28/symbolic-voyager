package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.SortContainer;
import com.github.a1k28.evoc.core.z3extended.sort.ArrayAndExprSort;
import com.github.a1k28.evoc.core.z3extended.sort.LinkedListNodeSort;
import com.github.a1k28.evoc.core.z3extended.sort.MapSort;
import com.microsoft.z3.*;

import java.util.HashMap;
import java.util.Map;

public class Z3CachingFactory {
    private final Context ctx;
    private final Map<String, Object> cache = new HashMap<>();

    public Z3CachingFactory(Context ctx) {
        this.ctx = ctx;
    }

    public MapSort mkMapSort(Sort key, Sort value) {
        return getMapSort(key, value).getSort();
    }

    public Expr mkMapSentinel(Sort key, Sort value) {
        return getMapSort(key, value).getSentinel();
    }

    public LinkedListNodeSort mkLinkedListSort(Sort keySort, Sort valueSort) {
        return getLinkedListSort(keySort, valueSort);
    }

    public ArrayAndExprSort mkArrayAndExprHolder(Sort arraySort, Sort exprSort) {
        return getArrayAndExprHolder(arraySort, exprSort);
    }

    private SortContainer<MapSort> getMapSort(Sort key, Sort value) {
        String keyName = key.getName().toString();
        String valueName = value.getName().toString();
        String strValue = "mapOptional:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (SortContainer<MapSort>) cache.get(strValue);

        TupleSort tupleSort = ctx.mkTupleSort(
                ctx.mkSymbol(strValue),
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
        MapSort sort = new MapSort(tupleSort);
        SortContainer container = new SortContainer(sort, sentinel);
        cache.put(strValue, container);
        return container;
    }

    private LinkedListNodeSort getLinkedListSort(Sort keySort, Sort valueSort) {
        String keyName = keySort.getName().toString();
        String valueName = valueSort.getName().toString();
        String strValue = "linkedList:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (LinkedListNodeSort) cache.get(strValue);

        TupleSort tupleSort = ctx.mkTupleSort(
                ctx.mkSymbol(strValue),
                new Symbol[]{
                        ctx.mkSymbol("value"),
                        ctx.mkSymbol("ref"),
                        ctx.mkSymbol("nextRef"),
                        ctx.mkSymbol("prevRef")},
                new Sort[]{valueSort, keySort, keySort, keySort}
        );
        LinkedListNodeSort sort = new LinkedListNodeSort(tupleSort);
        cache.put(strValue, sort);
        return sort;
    }

    private ArrayAndExprSort getArrayAndExprHolder(Sort arraySort, Sort exprSort) {
        String keyName = arraySort.getName().toString();
        String valueName = exprSort.getName().toString();
        String strValue = "arrayAndExprHolder:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (ArrayAndExprSort) cache.get(strValue);

        TupleSort tupleSort = ctx.mkTupleSort(
                ctx.mkSymbol(strValue),
                new Symbol[]{
                        ctx.mkSymbol("array"),
                        ctx.mkSymbol("expr")},
                new Sort[]{arraySort, exprSort}
        );
        ArrayAndExprSort sort = new ArrayAndExprSort(tupleSort);
        cache.put(strValue, sort);
        return sort;
    }
}
