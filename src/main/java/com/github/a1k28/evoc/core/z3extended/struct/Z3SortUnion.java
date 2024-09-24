package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.microsoft.z3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Z3SortUnion {
    private final Z3ExtendedContext ctx;
    private final DatatypeSort genericSort;
    private final Constructor[] constructors;
    private final Map<Sort, Integer> typeMap = new HashMap<>();

    public Z3SortUnion(Z3ExtendedContext ctx) {
        this.ctx = ctx;
        // Create the union datatype as before
        constructors = new Constructor[12];
        constructors[0] = ctx.mkConstructor("intType", "isIntKey", new String[] { "intValue" }, new Sort[] { ctx.getIntSort() }, null);
        constructors[1] = ctx.mkConstructor("stringKey", "isStringKey", new String[] { "stringValue" }, new Sort[] { ctx.getStringSort() }, null);
        constructors[2] = ctx.mkConstructor("boolType", "isBoolKey", new String[] { "boolValue" }, new Sort[] { ctx.getBoolSort() }, null);
        constructors[3] = ctx.mkConstructor("realType", "isRealKey", new String[] { "realValue" }, new Sort[] { ctx.getRealSort() }, null);
        constructors[4] = ctx.mkConstructor("charType", "isCharKey", new String[] { "charValue" }, new Sort[] { ctx.mkCharSort() }, null);
        constructors[5] = ctx.mkConstructor("arrayType", "isArrayKey", new String[] { "arrayValue" }, new Sort[] { SortType.ARRAY.value(ctx) }, null);
        constructors[6] = ctx.mkConstructor("mapType", "isMapKey", new String[] { "mapValue" }, new Sort[] { SortType.MAP.value(ctx) }, null);
        constructors[7] = ctx.mkConstructor("setType", "isSetKey", new String[] { "setValue" }, new Sort[] { SortType.SET.value(ctx) }, null);
        constructors[8] = ctx.mkConstructor("objectType", "isObjectKey", new String[] { "objectValue" }, new Sort[] { SortType.OBJECT.value(ctx) }, null);
        constructors[9] = ctx.mkConstructor("nullType", "isNullKey", new String[] { "nullValue" }, new Sort[] { SortType.NULL.value(ctx) }, null);
        constructors[10] = ctx.mkConstructor("fp64Type", "isFP64Key", new String[] { "fp64Value" }, new Sort[] { ctx.mkFPSort64() }, null);
        constructors[11] = ctx.mkConstructor("unknownType", "isUnknownKey", new String[] { "unknownValue" }, new Sort[] { SortType.UNKNOWN.value(ctx) }, null);
        genericSort = ctx.mkDatatypeSort("sortUnion", constructors);
    }

    public Expr wrapValue(Expr expr) {
        int idx = getUnwrappedTypeIdx(expr);
        return constructors[idx].ConstructorDecl().apply(expr);
    }

    public Expr unwrapValue(Expr expr, Expr defaultValue) {
        return unwrapValue(expr).orElse(defaultValue);
    }

    public Optional<Expr> unwrapValue(Expr expr) {
        Z3ExtendedSolver solver = ctx.getSolver();
        for (Constructor constructor : constructors) {
            FuncDecl tester = constructor.getTesterDecl();
            BoolExpr testValue = (BoolExpr) tester.apply(expr);
            if (solver.isUnsatisfiable(ctx.mkNot(testValue))) {
                FuncDecl accessor = constructor.getAccessorDecls()[0];
                return Optional.of(accessor.apply(expr));
            }
        }
        // if we have reached this point, then we have a sentinel
        return Optional.empty();
    }

    public DatatypeSort getGenericSort() {
        return this.genericSort;
    }

    private int getUnwrappedTypeIdx(Expr expr) {
        Sort sort = expr.getSort();
        if (typeMap.containsKey(sort))
            return typeMap.get(sort);
        int idx = getUnwrappedTypeIdx(sort);
        typeMap.put(sort, idx);
        return idx;
    }

    private int getUnwrappedTypeIdx(Sort sort) {
        Class type = sort.getClass();
        if (type == IntSort.class)
            return 0;
        if (type == SeqSort.class && "String".equals(sort.toString()))
            return 1;
        if (type == BoolSort.class)
            return 2;
        if (type == RealSort.class)
            return 3;
        if (type == CharSort.class)
            return 4;
        if (SortType.ARRAY.equals(sort))
            return 5;
        if (SortType.MAP.equals(sort))
            return 6;
        if (SortType.SET.equals(sort))
            return 7;
        if (SortType.OBJECT.equals(sort))
            return 8;
        if (SortType.NULL.equals(sort))
            return 9;
        if (type == FPSort.class)
            return 10;
        if (SortType.UNKNOWN.equals(sort))
            return 11;
        throw new RuntimeException("Undefined type for: " + sort);
    }
}
