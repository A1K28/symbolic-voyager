package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.SortContainer;
import com.github.a1k28.evoc.core.z3extended.sort.ArrayAndExprSort;
import com.github.a1k28.evoc.core.z3extended.sort.LinkedListNodeSort;
import com.github.a1k28.evoc.core.z3extended.sort.LinkedListSort;
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
        return getMapSort(key, value);
    }

    public LinkedListSort mkLinkedListSort(Sort referenceSort, ArraySort referenceMapSort) {
        return getLinkedListSort(referenceSort, referenceMapSort);
    }

    public LinkedListNodeSort mkLinkedListNodeSort(Sort keySort, Sort valueSort) {
        return getLinkedListNodeSort(keySort, valueSort);
    }

    public ArrayAndExprSort mkArrayAndExprHolder(Sort arraySort, Sort exprSort) {
        return getArrayAndExprHolder(arraySort, exprSort);
    }

    private MapSort getMapSort(Sort key, Sort value) {
        String keyName = key.getName().toString();
        String valueName = value.getName().toString();
        String strValue = "mapOptional:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (MapSort) cache.get(strValue);

        MapSort sort = new MapSort(ctx, strValue, key, value);
        cache.put(strValue, sort);
        return sort;
    }

    private LinkedListSort getLinkedListSort(Sort referenceSort, ArraySort referenceMapSort) {
        String keyName = referenceSort.getName().toString();
        String valueName = referenceMapSort.getName().toString();
        String strValue = "linkedList:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (LinkedListSort) cache.get(strValue);

        LinkedListSort sort = new LinkedListSort(ctx, strValue, referenceSort, referenceMapSort);
        cache.put(strValue, sort);
        return sort;
    }

    private LinkedListNodeSort getLinkedListNodeSort(Sort keySort, Sort valueSort) {
        String keyName = keySort.getName().toString();
        String valueName = valueSort.getName().toString();
        String strValue = "linkedListNode:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (LinkedListNodeSort) cache.get(strValue);

        LinkedListNodeSort sort = new LinkedListNodeSort(ctx, strValue, keySort, valueSort);
        cache.put(strValue, sort);
        return sort;
    }

    private ArrayAndExprSort getArrayAndExprHolder(Sort arraySort, Sort exprSort) {
        String keyName = arraySort.getName().toString();
        String valueName = exprSort.getName().toString();
        String strValue = "arrayAndExprHolder:"+keyName+":"+valueName;
        if (cache.containsKey(strValue))
            return (ArrayAndExprSort) cache.get(strValue);

        ArrayAndExprSort sort = new ArrayAndExprSort(ctx, strValue, arraySort, exprSort);
        cache.put(strValue, sort);
        return sort;
    }
}
