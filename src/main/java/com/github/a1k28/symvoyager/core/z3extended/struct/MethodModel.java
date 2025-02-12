package com.github.a1k28.symvoyager.core.z3extended.struct;

import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import com.github.a1k28.symvoyager.helper.Logger;
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

    // objects
    OBJECT_EQUALS(Object.class, "boolean equals(java.lang.Object)", true),

    // bytes
    BYTE_VALUE(Byte.class, "byte byteValue()", true),
    BYTE_VALUE_OF(Byte.class,"java.lang.Byte valueOf(java.lang.String)", false),
    BYTE_VALUE_OF_BYTE(Byte.class,"java.lang.Byte valueOf(byte)", false),
    BYTE_PARSE_BYTE(Byte.class,"byte parseByte(java.lang.String)", false),

    // shorts
    SHORT_VALUE(Short.class, "short shortValue()", true),
    SHORT_VALUE_OF(Short.class,"java.lang.Short valueOf(java.lang.String)", false),
    SHORT_VALUE_OF_SHORT(Short.class,"java.lang.Short valueOf(short)", false),
    SHORT_PARSE_SHORT(Short.class,"byte parseShort(java.lang.String)", false),

    // integers
    INT_VALUE(Integer.class, "int intValue()", true),
    INT_VALUE_OF(Integer.class,"java.lang.Integer valueOf(java.lang.String)", false),
    INT_VALUE_OF_INT(Integer.class,"java.lang.Integer valueOf(int)", false),
    INT_PARSE_INT(Integer.class,"int parseInt(java.lang.String)", false),

    // longs
    LONG_VALUE(Long.class, "long longValue()", true),
    LONG_VALUE_OF(Long.class, "java.lang.Long valueOf(java.lang.String)", false),
    LONG_VALUE_OF_LONG(Long.class, "java.lang.Long valueOf(long)", false),
    LONG_PARSE_LONG(Long.class, "long parseLong(java.lang.String)", false),

    // floats
    FLOAT_VALUE(Float.class, "float floatValue()", true),
    FLOAT_VALUE_OF(Float.class,"java.lang.Float valueOf(java.lang.String)", false),
    FLOAT_VALUE_OF_FLOAT(Float.class,"java.lang.Float valueOf(float)", false),
    FLOAT_PARSE_FLOAT(Float.class,"float parseFloat(java.lang.String)", false),

    // doubles
    DOUBLE_VALUE(Double.class, "double doubleValue()", true),
    DOUBLE_VALUE_OF(Double.class,"java.lang.Double valueOf(java.lang.String)", false),
    DOUBLE_VALUE_OF_DOUBLE(Double.class,"java.lang.Double valueOf(double)", false),
    DOUBLE_PARSE_DOUBLE(Double.class,"double parseDouble(java.lang.String)", false),

    // strings
    STRING_EQUALS(String.class,"boolean equals(java.lang.Object)",true),
    STRING_LEN(String.class,"int length()",true),
    STRING_STARTS_WITH(String.class, "boolean startsWith(java.lang.String)", true),
    STRING_ENDS_WITH(String.class, "boolean endsWith(java.lang.String)", true),
    STRING_COMPARE(String.class, "int compareTo(java.lang.String)", true),
    STRING_CHAR_AT(String.class, "char charAt(int)", true),
    STRING_INDEX_OF(String.class, "int indexOf(int)", true),
    STRING_SUBSTRING_1(String.class, "java.lang.String substring(int)", true),
    STRING_SUBSTRING_2(String.class, "java.lang.String substring(int,int)", true),
    STRING_CONTAINS(String.class, "boolean contains(java.lang.CharSequence)", true),
    STRING_SOOT_CONCAT_STRING(String.class,"<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String)>", false),
    STRING_SOOT_CONCAT_INT(String.class,"<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(int)>", false),
    STRING_VALUE_OF_BOOL(String.class, "java.lang.String valueOf(boolean)", false),
    STRING_VALUE_OF_BYTE(String.class, "java.lang.String valueOf(byte)", false),
    STRING_VALUE_OF_SHORT(String.class, "java.lang.String valueOf(short)", false),
    STRING_VALUE_OF_INT(String.class, "java.lang.String valueOf(int)", false),
    STRING_VALUE_OF_LONG(String.class, "java.lang.String valueOf(long)", false),
    STRING_VALUE_OF_FLOAT(String.class, "java.lang.String valueOf(float)", false),
    STRING_VALUE_OF_DOUBLE(String.class, "java.lang.String valueOf(double)", false),
    STRING_VALUE_OF_CHAR(String.class, "java.lang.String valueOf(char)", false),

    // --- lists ---
    LIST_INIT(List.class,"void <init>()", true),
    LIST_INIT_WITH_CAPACITY(List.class,"void <init>(int)", true),
    LIST_INIT_FILL_CAPACITY(List.class, UUID.randomUUID().toString(), true),
    LIST_INIT_WITH_COLLECTION(List.class,"void <init>(java.util.Collection)", true),
    LIST_SIZE(List.class,"int size()", true),
    LIST_IS_EMPTY(List.class,"boolean isEmpty()", true),
    LIST_ADD(List.class,"boolean add(java.lang.Object)", true),
    LIST_ADD_BY_INDEX(List.class,"void add(int,java.lang.Object)", true),
    LIST_ADD_ALL(List.class,"boolean addAll(java.util.Collection)", true),
    LIST_ADD_ALL_BY_INDEX(List.class,"boolean addAll(int,java.util.Collection)", true),
    LIST_REMOVE(List.class,"boolean remove(java.lang.Object)", true),
    LIST_REMOVE_BY_INDEX(List.class,"java.lang.Object remove(int)", true),
    LIST_REMOVE_ALL(List.class,"boolean removeAll(java.util.Collection)", true),
    LIST_CONTAINS(List.class,"boolean contains(java.lang.Object)", true),
    LIST_CONTAINS_ALL(List.class,"boolean containsAll(java.util.Collection)", true),
    LIST_RETAIN_ALL(List.class,"boolean retainAll(java.util.Collection)", true),
    LIST_CLEAR(List.class,"void clear()", true),
    LIST_EQUALS(List.class,"boolean equals(java.lang.Object)", true),
    LIST_GET(List.class,"java.lang.Object get(int)", true),
    LIST_SET(List.class,"java.lang.Object set(int,java.lang.Object)", true),
    LIST_HASH_CODE(List.class,"int hashCode()", true),
    LIST_SUBLIST(List.class,"java.util.List subList(int,int)", true),
    LIST_INDEX_OF(List.class,"int indexOf(java.lang.Object)", true),
//    LIST_ITERATOR(List.class, "java.util.Iterator iterator()", true),
//    LIST_STREAM(List.class, "java.util.stream.Stream stream()>", true),
    LIST_LAST_INDEX_OF(List.class,"int lastIndexOf(java.lang.Object)", true),
    LIST_OF_OBJECT_ARR(List.class,"java.util.List of(java.lang.Object[])", true),
    LIST_OF(List.class,"java.util.List of()", false),
    LIST_OF_1(List.class,"java.util.List of(java.lang.Object)", false),
    LIST_OF_2(List.class,"java.util.List of(java.lang.Object,java.lang.Object)", false),
    LIST_OF_3(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_4(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_5(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_6(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_7(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_8(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_9(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
    LIST_OF_10(List.class,"java.util.List of(java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object,java.lang.Object)", false),
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
    MAP_INIT_FROM_MAP(Map.class, "<java.util.HashMap: void <init>(java.util.Map)>", true),
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
    MAP_COPY_OF(Map.class, "java.util.Map copyOf(java.util.Map)", true),
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

    private static final Logger log = Logger.getInstance(MethodModel.class);
    private static final Map<String, List<MethodModel>> map = new HashMap<>();
    private static final String intRegex = "^[0-9]+$";

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

        // TODO: maybe change this??
        if (this.signature.matches(".*boolean equals\\(.*\\).*") && args.size() == 2) {
            boolean eq1 = SortType.NULL.equals(args.get(0).getSort());
            boolean eq2 = SortType.NULL.equals(args.get(1).getSort());
            if (eq1 && eq2) return ctx.mkBool(true);
            if (eq1 ^ eq2) return ctx.mkBool(false);
        }

        return switch (this) {
//            case SOOT_COMPARE -> ctx.mkInt(1);
//            case SOOT_UNARY_OPERATOR_APPLY -> ctx.mkInt(0);

            case OBJECT_EQUALS -> ctx.mkEq(args.get(0), args.get(1));

            case BYTE_VALUE,
                    BYTE_VALUE_OF_BYTE,
                    SHORT_VALUE,
                    SHORT_VALUE_OF_SHORT,
                    INT_VALUE,
                    INT_VALUE_OF_INT,
                    LONG_VALUE,
                    LONG_VALUE_OF_LONG,
                    FLOAT_VALUE,
                    FLOAT_VALUE_OF_FLOAT,
                    DOUBLE_VALUE,
                    DOUBLE_VALUE_OF_DOUBLE -> args.get(0);

            case BYTE_VALUE_OF,
                    BYTE_PARSE_BYTE,
                    SHORT_VALUE_OF,
                    SHORT_PARSE_SHORT,
                    INT_VALUE_OF,
                    INT_PARSE_INT,
                    LONG_VALUE_OF,
                    LONG_PARSE_LONG,
                    FLOAT_VALUE_OF,
                    FLOAT_PARSE_FLOAT,
                    DOUBLE_VALUE_OF,
                    DOUBLE_PARSE_DOUBLE -> ctx.stringToInt(args.get(0));

            case STRING_EQUALS -> ctx.mkEq(args.get(0), args.get(1));
            case STRING_LEN -> ctx.mkLength(args.get(0));
            case STRING_STARTS_WITH -> ctx.mkPrefixOf(args.get(1), args.get(0));
            case STRING_ENDS_WITH -> ctx.mkSuffixOf(args.get(1), args.get(0));
            case STRING_COMPARE -> ctx.mkStringCompare(args.get(0), args.get(1));
            case STRING_CHAR_AT -> ctx.charToInt(ctx.mkNth(args.get(0), args.get(1)));
            case STRING_INDEX_OF -> ctx.mkIndexOf(args.get(0), args.get(0), args.get(1));
            case STRING_SUBSTRING_1 -> ctx.mkExtract(args.get(0), args.get(1), ctx.mkLength(args.get(0)));
            case STRING_SUBSTRING_2 -> ctx.mkExtract(args.get(0), args.get(1), args.get(2));
            case STRING_CONTAINS -> ctx.mkContains(args.get(0), args.get(1));
            case STRING_SOOT_CONCAT_STRING -> ctx.mkStringConcatString(args.get(0), args.get(1));
            case STRING_SOOT_CONCAT_INT -> ctx.mkStringConcatInt(args.get(0), args.get(1));
            case STRING_VALUE_OF_BOOL,
                    STRING_VALUE_OF_BYTE,
                    STRING_VALUE_OF_SHORT,
                    STRING_VALUE_OF_INT,
                    STRING_VALUE_OF_LONG,
                    STRING_VALUE_OF_FLOAT -> ctx.intToString(args.get(0));
            case STRING_VALUE_OF_DOUBLE -> ctx.mkString(args.get(0).getString());
            case STRING_VALUE_OF_CHAR -> ctx.mkString(args.get(0).getString());

//            case SET_RETAIN_ALL -> ctx.mkSetIntersection(args.get(0), args.get(1));
//            case SET_ADD -> ctx.mkSetAdd(args.get(0), args.get(1));
//            case SET_LEN -> ctx.mkSetLength(args.get(0));
//            case SET_CONTAINS -> ctx.mkSetContains(args.get(0), args.get(1));
//            case SET_REMOVE -> ctx.mkSetRemove(args.get(0), args.get(1));

            case LIST_INIT -> ctx.getLinkedListInstance().constructor(args.get(0));
            case LIST_INIT_WITH_CAPACITY -> ctx.getLinkedListInstance().constructor(args.get(0), (IntExpr) args.get(1));
            case LIST_INIT_FILL_CAPACITY -> ctx.getLinkedListInstance().constructor(args.get(0), (IntExpr) args.get(1), true);
            case LIST_INIT_WITH_COLLECTION -> ctx.getLinkedListInstance().constructor(args.get(0), args.get(1));
            case LIST_ADD -> ctx.getLinkedListInstance().add(args.get(0), args.get(1));
            case LIST_ADD_BY_INDEX -> ctx.getLinkedListInstance().add(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_ADD_ALL -> ctx.getLinkedListInstance().addAll(args.get(0), args.get(1));
            case LIST_ADD_ALL_BY_INDEX -> ctx.getLinkedListInstance().addAll(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_GET -> ctx.getLinkedListInstance().get(args.get(0), (IntExpr) args.get(1));
            case LIST_REMOVE_BY_INDEX -> ctx.getLinkedListInstance().remove(args.get(0), (IntExpr) args.get(1));
            case LIST_REMOVE -> ctx.getLinkedListInstance().remove(args.get(0), args.get(1));
            case LIST_REMOVE_ALL -> ctx.getLinkedListInstance().removeAll(args.get(0), args.get(1));
            case LIST_RETAIN_ALL -> ctx.getLinkedListInstance().retainAll(args.get(0), args.get(1));
            case LIST_EQUALS -> ctx.getLinkedListInstance().equals(args.get(0), args.get(1));
            case LIST_SET -> ctx.getLinkedListInstance().set(args.get(0), (IntExpr) args.get(1), args.get(2));
            case LIST_HASH_CODE -> ctx.getLinkedListInstance().hashCode(args.get(0));
            case LIST_SUBLIST -> null;
            case LIST_INDEX_OF -> ctx.getLinkedListInstance().indexOf(args.get(0), args.get(1));
            case LIST_LAST_INDEX_OF -> ctx.getLinkedListInstance().lastIndexOf(args.get(0), args.get(1));
            case LIST_OF_OBJECT_ARR -> ctx.getLinkedListInstance().constructorFrom(args.get(0));
            case LIST_SIZE -> ctx.getLinkedListInstance().size(args.get(0));
            case LIST_IS_EMPTY -> ctx.getLinkedListInstance().isEmpty(args.get(0));
            case LIST_CONTAINS -> ctx.getLinkedListInstance().contains(args.get(0), args.get(1));
            case LIST_CONTAINS_ALL -> ctx.getLinkedListInstance().containsAll(args.get(0), args.get(1));
            case LIST_CLEAR -> ctx.getLinkedListInstance().clear(args.get(0));
            case LIST_OF, LIST_OF_1, LIST_OF_2, LIST_OF_3, LIST_OF_4,
                    LIST_OF_5, LIST_OF_6, LIST_OF_7, LIST_OF_8, LIST_OF_9, LIST_OF_10
                    -> ctx.getLinkedListInstance().constructorOf(args);

            case MAP_INIT -> ctx.getMapInstance().constructor(args.get(0));
            case MAP_INIT_FROM_MAP -> ctx.getMapInstance().constructor(args.get(0), args.get(1));
            case MAP_GET -> ctx.getMapInstance().get(args.get(0), args.get(1));
            case MAP_PUT -> ctx.getMapInstance().put(args.get(0), args.get(1), args.get(2));
            case MAP_SIZE -> ctx.getMapInstance().size(args.get(0));
            case MAP_IS_EMPTY -> ctx.getMapInstance().isEmpty(args.get(0));
            case MAP_CONTAINS_KEY -> ctx.getMapInstance().containsKey(args.get(0), args.get(1));
            case MAP_CONTAINS_VALUE -> ctx.getMapInstance().containsValue(args.get(0), args.get(1));
            case MAP_REMOVE -> ctx.getMapInstance().remove(args.get(0), args.get(1));
            case MAP_PUT_ALL -> ctx.getMapInstance().putAll(args.get(0), args.get(1));
            case MAP_CLEAR -> ctx.getMapInstance().clear(args.get(0));
            case MAP_EQUALS -> ctx.getMapInstance().equals(args.get(0), args.get(1));
            case MAP_GET_OR_DEFAULT -> ctx.getMapInstance().getOrDefault(args.get(0), args.get(1), args.get(2));
            case MAP_PUT_IF_ABSENT -> ctx.getMapInstance().putIfAbsent(args.get(0), args.get(1), args.get(2));
            case MAP_REMOVE_BY_KEY_AND_VALUE -> ctx.getMapInstance().removeByKeyAndValue(args.get(0), args.get(1), args.get(2));
            case MAP_REPLACE_BY_KEY_AND_VALUE -> ctx.getMapInstance().replaceByKeyAndValue(args.get(0), args.get(1), args.get(2), args.get(3));
            case MAP_REPLACE -> ctx.getMapInstance().replace(args.get(0), args.get(1), args.get(2));
            case MAP_COPY_OF -> ctx.getMapInstance().copyOf(args.get(0));
            case MAP_OF, MAP_OF_1, MAP_OF_2, MAP_OF_3, MAP_OF_4, MAP_OF_5,
                    MAP_OF_6, MAP_OF_7, MAP_OF_8, MAP_OF_9, MAP_OF_10
                    -> ctx.getMapInstance().of(args.toArray(new Expr[0]));
        };
    }

    public static Optional<MethodModel> get(MethodSignature methodSignature) {
        if (map.containsKey(methodSignature.toString()))
            return Optional.of(map.get(methodSignature.toString()).get(0));

        Class<?> clazz = getClassFromSignature(methodSignature);
        String subSignature = methodSignature.getSubSignature().toString();
        if (!map.containsKey(subSignature)) return Optional.empty();
        List<MethodModel> methodModels = map.get(subSignature);
        List<MethodModel> suspects = new ArrayList<>();
        for (MethodModel methodModel : methodModels) {
            if (methodModel.clazz.isAssignableFrom(clazz))
                suspects.add(methodModel);
        }

        if (suspects.isEmpty())
            return Optional.empty();
        if (suspects.size() == 1)
            return Optional.of(suspects.get(0));

        // return the most specific implementation
        outer: for (int i = 0; i < suspects.size(); i++) {
            for (int j = 0; j < suspects.size(); j++) {
                if (i == j) continue;
                if (suspects.get(i).clazz.isAssignableFrom(suspects.get(j).clazz)) continue outer;
            }
            return Optional.of(suspects.get(i));
        }

        return Optional.of(suspects.get(0));
    }

    public boolean hasBase() {
        return hasBase;
    }

    private static Class<?> getClassFromSignature(MethodSignature methodSignature) {
        try {
            return Class.forName(methodSignature.getDeclClassType().toString());
        } catch (ClassNotFoundException e) {
            log.error("Class not found: " + e.getMessage());
            return Object.class;
        }
    }
}
