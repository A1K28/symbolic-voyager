package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.microsoft.z3.Expr;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public enum MethodModel {
    // strings
    STRING_EQUALS("<java.lang.String: boolean equals(java.lang.Object)>",true),
    STRING_LEN("<java.lang.String: int length()>",true),
    STRING_SOOT_CONCAT("<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String)>", false),

    // --- lists ---
    // size
    // isEmpty
    // add (element)
    // add (index, element)
    // addAll (list)
    // addAll (index, list)
    // remove (element)
    // remove (index)
    // removeAll
    // contains
    // containsAll
    // retainAll
    // clear
    // equals
    // hashCode
    // get
    // set
    // indexOf
    // lastIndexOf
    // replaceAll ?
    // sort ?
    // iterators ?
    // subList ?
    // stream ?

    // sets
    SET_RETAIN_ALL("<java.util.Set: boolean retainAll(java.util.Collection)>",true),
    SET_ADD("<java.util.Set: boolean add(java.lang.Object)>",true),
    SET_LEN("<java.util.Set: int size()>",true),
    SET_CONTAINS("<java.util.Set: boolean contains(java.lang.Object)>",true),
    SET_REMOVE("<java.util.Set: boolean remove(java.lang.Object)>", true);

    private static final Map<String, MethodModel> map = new HashMap<>();

    static {
        for (MethodModel e : MethodModel.values()) {
            map.put(e.signature, e);
        }
    }

    // i love you <3 mpua

    private final String signature;
    private final boolean hasBase;

    public Expr apply(Z3ExtendedContext ctx, String signature, List<Expr> args) {
        return switch (map.get(signature)) {
            case STRING_EQUALS -> ctx.mkEq(args.get(0), args.get(1));
            case STRING_LEN -> ctx.mkLength(args.get(0));
            case STRING_SOOT_CONCAT -> ctx.mkConcat(args.get(0), ctx.mkString(args.get(1).getString()));

            case SET_RETAIN_ALL -> ctx.mkSetIntersection(args.get(0), args.get(1));
            case SET_ADD -> ctx.mkSetAdd(args.get(0), args.get(1));
            case SET_LEN -> ctx.mkSetLength(args.get(0));
            case SET_CONTAINS -> ctx.mkSetContains(args.get(0), args.get(1));
            case SET_REMOVE -> ctx.mkSetRemove(args.get(0), args.get(1));
        };
    }

    public static MethodModel get(String signature) {
        return map.get(signature);
    }

    public static boolean contains(String signature) {
        return map.containsKey(signature);
    }

    public boolean hasBase() {
        return hasBase;
    }
}
