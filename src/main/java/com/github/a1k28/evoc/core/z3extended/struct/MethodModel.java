package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import lombok.RequiredArgsConstructor;
import sootup.core.signatures.MethodSignature;

import java.util.*;

@RequiredArgsConstructor
public enum MethodModel {
    // SOOT
//    SOOT_COMPARE("<sootup.dummy.InvokeDynamic: java.util.Comparator compare()>", true),
//    SOOT_UNARY_OPERATOR_APPLY("<sootup.dummy.InvokeDynamic: java.util.function.UnaryOperator apply()>", true),

    // Integers
    INT_VALUE(Integer.class, "int intValue()", true),
    INT_VALUE_OF(Integer.class,"java.lang.Integer valueOf(int)", false),

    // strings
    STRING_EQUALS(String.class,"boolean equals(java.lang.Object)",true),
    STRING_LEN(String.class,"int length()",true),
    STRING_SOOT_CONCAT(String.class,"<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String)>", false),

    // --- lists ---
//    LIST_INIT(List.class,"void <init>()", true),
//    LIST_INIT_WITH_CAPACITY(List.class,"void <init>(int)", true),
//    LIST_INIT_WITH_COLLECTION(List.class,"void <init>(java.util.Collection)", true),
//    LIST_SIZE(List.class,"int size()", true),
//    LIST_IS_EMPTY(List.class,"boolean isEmpty()", true),
//    LIST_ADD(List.class,"boolean add(java.lang.Object)", true),
//    LIST_ADD_BY_INDEX(List.class,"void add(int,java.lang.Object)", true),
//    LIST_ADD_ALL(List.class,"boolean addAll(java.util.Collection)", true),
//    LIST_ADD_ALL_BY_INDEX(List.class,"boolean addAll(int,java.util.Collection)", true),
//    LIST_REMOVE(List.class,"boolean remove(java.lang.Object)", true),
//    LIST_REMOVE_BY_INDEX(List.class,"java.lang.Object remove(int)", true),
//    LIST_REMOVE_ALL(List.class,"boolean removeAll(java.util.Collection)", true),
//    LIST_CONTAINS(List.class,"boolean contains(java.lang.Object)", true),
//    LIST_CONTAINS_ALL(List.class,"boolean containsAll(java.util.Collection)", true),
//    LIST_RETAIN_ALL(List.class,"boolean retainAll(java.util.Collection)", true),
//    LIST_CLEAR(List.class,"void clear()", true),
//    LIST_EQUALS(List.class,"boolean equals(java.lang.Object)", true),
//    LIST_GET(List.class,"java.lang.Object get(int)", true),
//    LIST_SET(List.class,"java.lang.Object set(int,java.lang.Object)", true),
//    LIST_HASH_CODE(List.class,"int hashCode()", true),
//    LIST_SUBLIST(List.class,"java.util.List subList(int,int)", true),
//    LIST_INDEX_OF(List.class,"int indexOf(java.lang.Object)", true),
////    LIST_STREAM("<java.util.List: java.util.stream.Stream stream()>", true),
//    LIST_LAST_INDEX_OF(List.class,"int lastIndexOf(java.lang.Object)", true),
//    LIST_OF_OBJECT_ARR(List.class,"java.util.List of(java.lang.Object[])", true),
//    LIST_OF(List.class,"java.util.List of()", true),
//    LIST_OF_1(List.class,"java.util.List of(java.lang.Object)", true),
//    LIST_OF_2(List.class,"java.util.List of(java.lang.Object,java.lang.Object)", true),
//    LIST_OF_3(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_4(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_5(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_6(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_7(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_8(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_9(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//    LIST_OF_10(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", true),
//
//    // replaceAll ?
//    // sort ?
//    // iterators ?
//    // stream ?
//
//    // sets
//    SET_RETAIN_ALL(Set.class,"boolean retainAll(java.util.Collection)",true),
//    SET_ADD(Set.class,"boolean add(java.lang.Object)",true),
//    SET_LEN(Set.class,"int size()",true),
//    SET_CONTAINS(Set.class,"boolean contains(java.lang.Object)",true),
//    SET_REMOVE(Set.class,"boolean remove(java.lang.Object)", true),

    // maps
    MAP_INIT(Map.class,"void <init>()", true),
    MAP_GET(Map.class, "java.lang.Object get(java.lang.Object)", true),
    MAP_PUT(Map.class, "java.lang.Object put(java.lang.Object,java.lang.Object)", true),
    MAP_SIZE(Map.class, "int size()", true),
    MAP_IS_EMPTY(Map.class, "boolean isEmpty()", true),
    MAP_CONTAINS_KEY(Map.class, "boolean containsKey(java.lang.Object)", true),
    MAP_CONTAINS_VALUE(Map.class, "boolean containsValue(java.lang.Object)", true),
    MAP_REMOVE(Map.class, "java.lang.Object remove(java.lang.Object)", true),
    MAP_PUT_ALL(Map.class, "void putAll(java.util.Map)", true),
    MAP_CLEAR(Map.class, "void clear()", true),
//    MAP_KEY_SET(Map.class, "java.util.Set keySet()", true),
//    MAP_VALUES(Map.class, "java.util.Collection values()", true),
//    MAP_ENTRY_SET(Map.class, "java.util.Set entrySet()", true),
//    MAP_PUT(Object.class, "<sootup.dummy.InvokeDynamic: java.util.function.Consumer accept()>", true),
//    MAP_FOR_EACH(Map.class, "void forEach(java.util.function.Consumer)", true),
    MAP_EQUALS(Map.class, "boolean equals(java.lang.Object)", true),
//    MAP_HASH_CODE(Map.class, "int hashCode()", true),
    MAP_GET_OR_DEFAULT(Map.class, "java.lang.Object getOrDefault(java.lang.Object,java.lang.Object)", true),
    MAP_PUT_IF_ABSENT(Map.class, "java.lang.Object putIfAbsent(java.lang.Object,java.lang.Object)", true),
    MAP_REMOVE_BY_KEY_AND_VALUE(Map.class, "boolean remove(java.lang.Object,java.lang.Object)", true),
    MAP_REPLACE_BY_KEY_AND_VALUE(Map.class, "boolean replace(java.lang.Object,java.lang.Object,java.lang.Object)", true),
    MAP_REPLACE(Map.class, "java.lang.Object replace(java.lang.Object,java.lang.Object)", true),
//    MAP_OF_ENTRIES(Map.class, "java.util.Map ofEntries(java.util.Map$Entry[])", false),
//    MAP_ENTRY(Map.class, "java.util.Map$Entry entry(java.lang.Object,java.lang.Object)", false),
    MAP_COPY_OF(Map.class, "java.util.Map copyOf(java.util.Map)", false),
    MAP_OF(Map.class, "java.util.Map of()", false),
    MAP_OF_1(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object)", false),
    MAP_OF_2(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    MAP_OF_3(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    MAP_OF_4(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_5(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_6(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_7(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_8(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_9(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false),
    MAP_OF_10(Map.class, "java.util.Map of(java.lang.Object,java.lang.Object),java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object", false);

    // computeIfAbsent
    // computeIfPresent
    // compute
    // merge
    // forEach
    // replaceAll
    // entry.comparingByKey
    // entry.comparingByValue
    // entry.copyOf

    private static final Map<String, List<MethodModel>> map = new HashMap<>();

    static {
        for (MethodModel e : MethodModel.values()) {
            if (!map.containsKey(e.signature)) map.put(e.signature, new ArrayList<>());
            map.get(e.signature).add(e);
        }
    }

    // i love you <3 mpua

    private final Class<?> clazz;
    private final String signature;
    private final boolean hasBase;

    public Expr apply(Z3ExtendedContext ctx, List<Expr> args) {
        System.out.println("APPLYING: " + clazz.getName() + ": " + signature);
        return switch (this) {
//            case SOOT_COMPARE -> ctx.mkInt(1);
//            case SOOT_UNARY_OPERATOR_APPLY -> ctx.mkInt(0);

            case INT_VALUE -> args.get(0);
            case INT_VALUE_OF -> args.get(0);

            case STRING_EQUALS -> ctx.mkEq(args.get(0), args.get(1));
            case STRING_LEN -> ctx.mkLength(args.get(0));
            case STRING_SOOT_CONCAT -> ctx.mkConcat(args.get(0), ctx.mkString(args.get(1).getString()));

//            case LIST_INIT, LIST_OF_OBJECT_ARR -> ctx.mkList(args.get(0));
//            case LIST_INIT_WITH_CAPACITY -> ctx.mkListWithCapacity(args.get(0), (IntExpr) args.get(1));
//            case LIST_INIT_WITH_COLLECTION -> ctx.mkListWithCollection(args.get(0), args.get(1));
//            case LIST_SIZE -> ctx.mkListLength(args.get(0));
//            case LIST_IS_EMPTY -> ctx.mkListIsEmpty(args.get(0));
//            case LIST_ADD -> ctx.mkListAdd(args.get(0), args.get(1));
//            case LIST_ADD_BY_INDEX -> ctx.mkListAdd(args.get(0), (IntExpr) args.get(1), args.get(2));
//            case LIST_ADD_ALL -> ctx.mkListAddAll(args.get(0), args.get(1));
//            case LIST_ADD_ALL_BY_INDEX -> ctx.mkListAddAll(args.get(0), (IntExpr) args.get(1), args.get(2));
//            case LIST_REMOVE -> ctx.mkListRemove(args.get(0), args.get(1));
//            case LIST_REMOVE_BY_INDEX -> ctx.mkListRemove(args.get(0), (IntExpr) args.get(1));
//            case LIST_REMOVE_ALL -> ctx.mkListRemoveAll(args.get(0), args.get(1));
//            case LIST_CONTAINS -> ctx.mkListContains(args.get(0), args.get(1));
//            case LIST_CONTAINS_ALL -> ctx.mkListContainsAll(args.get(0), args.get(1));
//            case LIST_RETAIN_ALL -> ctx.mkListRetainAll(args.get(0), args.get(1));
//            case LIST_CLEAR -> ctx.mkListClear(args.get(0));
//            case LIST_EQUALS -> ctx.mkListEquals(args.get(0), args.get(1));
//            case LIST_GET -> ctx.mkListGet(args.get(0), (IntExpr) args.get(1));
//            case LIST_SET -> ctx.mkListSet(args.get(0), (IntExpr) args.get(1), args.get(2));
//            case LIST_HASH_CODE -> ctx.mkListHashCode(args.get(0));
//            case LIST_SUBLIST -> ctx.mkListSublist(args.get(0), (IntExpr) args.get(1), (IntExpr) args.get(2));
//            case LIST_INDEX_OF -> ctx.mkListIndexOf(args.get(0), args.get(1));
//            case LIST_LAST_INDEX_OF -> ctx.mkListLastIndexOf(args.get(0), args.get(1));
//            case LIST_OF, LIST_OF_1, LIST_OF_2, LIST_OF_3, LIST_OF_4, LIST_OF_5,
//                    LIST_OF_6, LIST_OF_7, LIST_OF_8, LIST_OF_9, LIST_OF_10
//                    -> ctx.mkListWithCollection(args);
//
//            case SET_RETAIN_ALL -> ctx.mkSetIntersection(args.get(0), args.get(1));
//            case SET_ADD -> ctx.mkSetAdd(args.get(0), args.get(1));
//            case SET_LEN -> ctx.mkSetLength(args.get(0));
//            case SET_CONTAINS -> ctx.mkSetContains(args.get(0), args.get(1));
//            case SET_REMOVE -> ctx.mkSetRemove(args.get(0), args.get(1));

            case MAP_INIT -> ctx.mkMapInit(args.get(0));
            case MAP_GET -> ctx.mkMapGet(args.get(0), args.get(1));
            case MAP_PUT -> ctx.mkMapPut(args.get(0), args.get(1), args.get(2));
            case MAP_SIZE -> ctx.mkMapLength(args.get(0));
            case MAP_IS_EMPTY -> ctx.mkMapIsEmpty(args.get(0));
            case MAP_CONTAINS_KEY -> ctx.mkMapContainsKey(args.get(0), args.get(1));
            case MAP_CONTAINS_VALUE -> ctx.mkMapContainsValue(args.get(0), args.get(1));
            case MAP_REMOVE -> ctx.mkMapRemove(args.get(0), args.get(1));
            case MAP_PUT_ALL -> ctx.mkMapPutAll(args.get(0), args.get(1));
            case MAP_CLEAR -> ctx.mkMapClear(args.get(0));
            case MAP_EQUALS -> ctx.mkMapEquals(args.get(0), args.get(1));
            case MAP_GET_OR_DEFAULT -> ctx.mkMapGetOrDefault(args.get(0), args.get(1), args.get(2));
            case MAP_PUT_IF_ABSENT -> ctx.mkMapPutIfAbsent(args.get(0), args.get(1), args.get(2));
            case MAP_REMOVE_BY_KEY_AND_VALUE -> ctx.mkMapRemove(args.get(0), args.get(1), args.get(2));
            case MAP_REPLACE_BY_KEY_AND_VALUE -> ctx.mkMapReplace(args.get(0), args.get(1), args.get(2), args.get(3));
            case MAP_REPLACE -> ctx.mkMapReplace(args.get(0), args.get(1), args.get(2));
            case MAP_COPY_OF -> ctx.mkMapCopyOf(args.get(0));
            case MAP_OF, MAP_OF_1, MAP_OF_2, MAP_OF_3, MAP_OF_4, MAP_OF_5,
                    MAP_OF_6, MAP_OF_7, MAP_OF_8, MAP_OF_9, MAP_OF_10
                    -> ctx.mkMapOf(args);
        };
    }

    public static Optional<MethodModel> get(MethodSignature methodSignature) {
        if (map.containsKey(methodSignature.toString()))
            return Optional.of(map.get(methodSignature.toString()).get(0));

        Class<?> clazz = getClassFromSignature(methodSignature);
        String subSignature = methodSignature.getSubSignature().toString();
        if (!map.containsKey(subSignature)) return Optional.empty();
        List<MethodModel> methodModels = map.get(subSignature);
        for (MethodModel methodModel : methodModels) {
            if (methodModel.clazz.isAssignableFrom(clazz))
                return Optional.of(methodModel);
        }

        return Optional.empty();
    }

    public boolean hasBase() {
        return hasBase;
    }

    private static Class<?> getClassFromSignature(MethodSignature methodSignature) {
        try {
            return Class.forName(methodSignature.getDeclClassType().toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return Object.class;
        }
    }
}
