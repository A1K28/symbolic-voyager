package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.microsoft.z3.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Z3Helper {
    private static final Map<Sort, Expr> sentinels = new HashMap<>();
    private static final Map<Sort, TupleSort> sortMap = new HashMap<>();

    public static TupleSort mapSort(Context ctx, Sort sort) {
        if (!sortMap.containsKey(sort)) {
            TupleSort tupleSort = ctx.mkTupleSort(
                    ctx.mkSymbol("optional:"+sort),
                    new Symbol[]{ctx.mkSymbol("value"), ctx.mkSymbol("isEmpty")},
                    new Sort[]{sort, ctx.getBoolSort()}
            );
            sortMap.put(sort, tupleSort);
        }
        return sortMap.get(sort);
    }

    public static Expr mkSentinel(Context ctx, Sort sort) {
        if (!sentinels.containsKey(sort)) {
            Expr sentinel = mapSort(ctx, sort).mkDecl().apply(
                    ctx.mkConst("sentinel", sort), ctx.mkTrue());
            sentinels.put(sort, sentinel);
        }
        return sentinels.get(sort);
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
}
