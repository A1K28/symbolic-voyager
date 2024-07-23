package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
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
    LIST_INIT("<java.util.ArrayList: void <init>()>", true),
    LIST_INIT_WITH_CAPACITY("<java.util.ArrayList: void <init>(int)>", true),
    LIST_INIT_WITH_COLLECTION("<java.util.ArrayList: void <init>(java.util.Collection)>", true),
    LIST_SIZE("<java.util.List: int size()>", true),
    LIST_IS_EMPTY("<java.util.List: boolean isEmpty()>", true),
    LIST_ADD("<java.util.List: boolean add(java.lang.Object)>", true),
    LIST_ADD_BY_INDEX("<java.util.List: void add(int,java.lang.Object)>", true),
    LIST_ADD_ALL("<java.util.List: boolean addAll(java.util.Collection)>", true),
    LIST_ADD_ALL_BY_INDEX("<java.util.List: boolean addAll(int,java.util.Collection)>", true),
    LIST_REMOVE("<java.util.List: boolean remove(java.lang.Object)>", true),
    LIST_REMOVE_BY_INDEX("<java.util.List: java.lang.Object remove(int)>", true),
    LIST_REMOVE_ALL("<java.util.List: boolean removeAll(java.util.Collection)>", true),
    LIST_CONTAINS("<java.util.List: boolean contains(java.lang.Object)>", true),
    LIST_CONTAINS_ALL("<java.util.List: boolean containsAll(java.util.Collection)>", true),
    LIST_RETAIN_ALL("<java.util.List: boolean retainAll(java.util.Collection)>", true),
    LIST_CLEAR("<java.util.List: void clear()>", true),
    LIST_EQUALS("<java.util.List: boolean equals(java.lang.Object)>", true),
    LIST_GET("<java.util.List: java.lang.Object get(int)>", true),
    LIST_SET("<java.util.List: java.lang.Object set(int,java.lang.Object)>", true),
    LIST_INDEX_OF("<java.util.List: int indexOf(java.lang.Object)>", true),
    LIST_LAST_INDEX_OF("<java.util.List: int lastIndexOf(java.lang.Object)>", true),
    LIST_OF("<java.util.List: java.util.List of()>", true),
    LIST_OF_1("<java.util.List: java.util.List of(java.lang.Object)>", true),
    LIST_OF_2("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object)>", true),
    LIST_OF_3("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_4("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_5("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_6("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_7("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_8("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_9("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),
    LIST_OF_10("<java.util.List: java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)>", true),

    // hashCode ?
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
        System.out.println("APPLYING: " + signature);
        return switch (map.get(signature)) {
            case STRING_EQUALS -> ctx.mkEq(args.get(0), args.get(1));
            case STRING_LEN -> ctx.mkLength(args.get(0));
            case STRING_SOOT_CONCAT -> ctx.mkConcat(args.get(0), ctx.mkString(args.get(1).getString()));

            case LIST_INIT -> ctx.mkList(args.get(0));
            case LIST_INIT_WITH_CAPACITY -> ctx.mkListWithCapacity(args.get(0), (IntExpr) args.get(1));
            case LIST_INIT_WITH_COLLECTION -> ctx.mkListWithCollection(args.get(0), args.get(1));
            case LIST_SIZE -> ctx.mkListLength(args.get(0));
            case LIST_IS_EMPTY -> ctx.mkListIsEmpty(args.get(0));
            case LIST_ADD -> ctx.mkListAdd(args.get(0), args.get(1));
            case LIST_ADD_BY_INDEX -> ctx.mkListAdd(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_ADD_ALL -> ctx.mkListAddAll(args.get(0), args.get(1));
            case LIST_ADD_ALL_BY_INDEX -> ctx.mkListAddAll(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_REMOVE -> ctx.mkListRemove(args.get(0), args.get(1));
            case LIST_REMOVE_BY_INDEX -> ctx.mkListRemove(args.get(0), (IntExpr) args.get(1));
            case LIST_REMOVE_ALL -> ctx.mkListRemoveAll(args.get(0), args.get(1));
            case LIST_CONTAINS -> ctx.mkListContains(args.get(0), args.get(1));
            case LIST_CONTAINS_ALL -> ctx.mkListContainsAll(args.get(0), args.get(1));
            case LIST_RETAIN_ALL -> ctx.mkListRetainAll(args.get(0), args.get(1));
            case LIST_CLEAR -> ctx.mkListClear(args.get(0));
            case LIST_EQUALS -> ctx.mkListEquals(args.get(0), args.get(1));
            case LIST_GET -> ctx.mkListGet(args.get(0), (IntExpr) args.get(1));
            case LIST_SET -> ctx.mkListSet(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_INDEX_OF -> ctx.mkListIndexOf(args.get(0), args.get(1));
            case LIST_LAST_INDEX_OF -> ctx.mkListLastIndexOf(args.get(0), args.get(1));
            case LIST_OF, LIST_OF_1, LIST_OF_2, LIST_OF_3, LIST_OF_4, LIST_OF_5,
                    LIST_OF_6, LIST_OF_7, LIST_OF_8, LIST_OF_9, LIST_OF_10
                    -> ctx.mkListWithCollection(args);

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
