package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.model.Tuple;
import com.github.a1k28.evoc.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.github.a1k28.evoc.core.z3extended.sort.MapSort;
import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;

public class Z3MapInstance implements IStack {
    private final Z3ExtendedContext ctx;
    private final Z3CachingFactory sortState;
    private final Z3SortUnion sortUnion;
    private final Z3Stack<Expr, MapModel> stack;
    private final Z3Stack<Expr, Tuple<Expr>> discoveredKeys;
    private final Z3ExtendedSolver solver;
    private final Expr sentinel;
    private final MapSort mapSort;

    public Z3MapInstance(Z3ExtendedContext context,
                         Z3ExtendedSolver solver,
                         Z3CachingFactory sortState,
                         Z3SortUnion sortUnion) {
        this.ctx = context;
        this.sortState = sortState;
        this.sortUnion = sortUnion;
        this.solver = solver;
        this.stack = new Z3Stack<>();
        this.discoveredKeys = new Z3Stack<>();

        Sort keySort = sortUnion.getGenericSort();
        Sort valueSort = sortUnion.getGenericSort();
        this.mapSort = sortState.mkMapSort(keySort, valueSort);
        this.sentinel = sortState.mkMapSentinel(keySort, valueSort);
    }

    @Override
    public void push() {
        stack.push();
        discoveredKeys.push();
    }

    @Override
    public void pop() {
        stack.pop();
        discoveredKeys.pop();
    }

    public MapSort getMapSort() {
        return this.mapSort;
    }

    public Expr constructor(Expr var1) {
        MapModel mapModel = constructor(var1, false);
        stack.add(var1, mapModel);
        return var1;
    }

    public Expr constructor(Expr var1, Expr var2) {
        MapModel newModel = new MapModel(getModel(var2));
        newModel.setReference(var1);
        stack.add(var1, newModel);
        return var1;

    }

    private MapModel constructor(Expr reference, boolean isSizeUnknown) {
        // TODO: handle sorts?
        Sort keySort = sortUnion.getGenericSort();
        Sort valueSort = sortUnion.getGenericSort();

        ArrayExpr array;
        ArithExpr size;
        if (isSizeUnknown) {
            size = (ArithExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            array = mkArray(keySort, mapSort.getSort());

            // size assertion
            BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
            solver.add(sizeAssertion);
        } else {
            size = ctx.mkInt(0);
            array = mkEmptyArray(keySort, sentinel);
        }

        return new MapModel(reference, array, mapSort.getSort(), size, isSizeUnknown);
    }

    public Optional<MapModel> getInitialMap(Expr var1) {
        return stack.getFirst(var1);
    }

    public List<Tuple<Expr>> getDiscoverableKeys() {
        return discoveredKeys.getAll();
    }

    public Expr get(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        Expr retrieved = getByKey(model, key);
        BoolExpr isEmpty = mapSort.isEmpty(retrieved);
        Expr res = ctx.mkITE(isEmpty, sentinel, retrieved);
        return sortUnion.unwrapValue(
                mapSort.getValue(res), mapSort.getValue(sentinel));
    }

    public Expr getOrDefault(Expr var1, Expr key, Expr def) {
        MapModel model = getModel(var1);
        Expr retrieved = getByKey(model, key);
        BoolExpr isEmpty = mapSort.isEmpty(retrieved);
        Expr unwrapped = sortUnion.unwrapValue(
                mapSort.getValue(retrieved), mapSort.getValue(sentinel));
        return ctx.mkITE(isEmpty, def, unwrapped);
    }

    public Expr put(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, false);
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, true);
    }

    public Expr size(Expr var1) {
        MapModel model = getModel(var1);
        return size(model);
    }

    public Expr initialSize(Expr var1) {
        MapModel model = getInitialMap(var1).orElseThrow();
        return size(model);
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr containsKey(Expr var1, Expr key) {
        return containsKey(getModel(var1), key);
    }

    public BoolExpr containsKey(MapModel model, Expr key) {
        addDiscoveredKey(key);
        Expr keyWrapped = sortUnion.wrapValue(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyCondition(model, retrieved, keyWrapped);
        return exists;
    }

    public BoolExpr containsWrappedKey(MapModel model, Expr keyWrapped) {
        Expr retrieved = ctx.mkSelect(model.getArray(), keyWrapped);
        return existsByKeyCondition(model, retrieved, keyWrapped);
    }

    public BoolExpr containsValue(Expr var1, Expr value) {
        MapModel model = getModel(var1);
        Expr keyWrapped = ctx.mkFreshConst("key", mapSort.getKeySort());
        Expr valueWrapped = sortUnion.wrapValue(value);
        Expr retrieved = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, keyWrapped, valueWrapped);

        BoolExpr existsMatch = ctx.mkExists(
                new Expr[]{keyWrapped},
                exists,
                1, null, null, null, null
        );
        solver.add(ctx.mkImplies(existsMatch, exists));

        addUndiscoveredKey(keyWrapped);
        return existsMatch;
    }

    public Expr remove(Expr var1, Expr key) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();
        Expr keyWrapped = sortUnion.wrapValue(key);

        Expr retrieved = ctx.mkSelect(map, keyWrapped);
        BoolExpr exists = existsByKeyCondition(model, retrieved, keyWrapped);
        Expr previous = ctx.mkITE(exists, retrieved, sentinel);
        Expr previousValue = mapSort.getValue(previous);

        map = ctx.mkStore(map, keyWrapped, sentinel);

        ArithExpr size = (ArithExpr) ctx.mkITE(exists,
                ctx.mkSub(model.getSize(), ctx.mkInt(1)), model.getSize());

        model.setArray(map);
        model.setSize(size);
        addDiscoveredKey(key);

        return sortUnion.unwrapValue(previousValue, ctx.mkNull());
    }

    public BoolExpr removeByKeyAndValue(Expr var1, Expr key, Expr value) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();
        Expr keyWrapped = sortUnion.wrapValue(key);
        Expr valueWrapped = sortUnion.wrapValue(value);

        Expr previousValue = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyAndValueCondition(model, previousValue, keyWrapped, valueWrapped);

        Expr newValue = ctx.mkITE(exists, sentinel, previousValue);
        map = ctx.mkStore(map, keyWrapped, newValue);

        model.setArray(map);
        addDiscoveredKey(key);

        return exists;
    }

    public Expr putAll(Expr var1, Expr var2) {
        MapModel target = copyModel(getModel(var1));
        MapModel source = getModel(var2);

        // we consider 4 different options for the sake of NECESSARY optimization
        if (target.isSizeUnknown() && !source.isSizeUnknown())
            putAllTKSK(target, source); // TUSK == TKSK
        else if (target.isSizeUnknown() && source.isSizeUnknown())
            putAllTUSU(target, source);
        else if (!target.isSizeUnknown() && !source.isSizeUnknown())
            putAllTKSK(target, source);
        else if (!target.isSizeUnknown() && source.isSizeUnknown())
            putAllTKSU(target, source);

        return null;
    }

    public Expr clear(Expr var1) {
        MapModel model = getModel(var1);
        model = copyModel(model);
        ArrayExpr updatedArray = mkEmptyArray(mapSort.getKeySort(), sentinel);
        model.setArray(updatedArray);
        model.setSize(ctx.mkInt(0));
        model.setSizeUnknown(false);
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        MapModel m1 = getModel(var1);
        MapModel m2 = getModel(var2);
        return ctx.mkEq(m1.getArray(), m2.getArray());
    }

    public BoolExpr replaceByKeyAndValue(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr keyWrapped = sortUnion.wrapValue(key);
        Expr oldValueWrapped = sortUnion.wrapValue(oldValue);
        Expr newValueWrapped = sortUnion.wrapValue(newValue);

        Expr retrieved = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, keyWrapped, oldValueWrapped);
        Expr value = mapSort.mkDecl(keyWrapped, newValueWrapped, ctx.mkBool(false));
        map = (ArrayExpr) ctx.mkITE(exists, ctx.mkStore(map, keyWrapped, value), map);

        model.setArray(map);
        addDiscoveredKey(key);

        return exists;
    }

    public Expr replace(Expr var1, Expr key, Expr value) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr keyWrapped = sortUnion.wrapValue(key);
        Expr valueWrapped = sortUnion.wrapValue(value);

        Expr previousValue = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyCondition(model, previousValue, keyWrapped);

        Expr newValue = mapSort.mkDecl(keyWrapped, valueWrapped, ctx.mkBool(false));
        newValue = ctx.mkITE(exists, newValue, sentinel);

        map = ctx.mkStore(map, keyWrapped, newValue);

        model.setArray(map);
        addDiscoveredKey(key);

        return mapSort.getValue(previousValue);
    }

    public Expr copyOf(Expr var1) {
        MapModel source = getModel(var1, false);
        MapModel target = new MapModel(source);
        ArrayExpr arr = mkArray(mapSort.getKeySort(), mapSort.getValueSort());
        target.setArray(arr);
        solver.add(ctx.mkEq(source.getArray(), arr));
        stack.add(target.getReference(), target);
        return target.getReference();
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(ctx.mkFreshConst(
                "Map", SortType.MAP.value(ctx)), false);
        ArrayExpr map = model.getArray();
        ArrayExpr set = ctx.mkEmptySet(mapSort.getKeySort());
        ArithExpr size = ctx.mkInt(0);
        if (vars != null) {
            for (int i = 0; i < vars.length; i+=2) {
                Expr key = sortUnion.wrapValue(vars[i]);
                Expr val = sortUnion.wrapValue(vars[i+1]);
                Expr value = mapSort.mkDecl(key, val, ctx.mkBool(false));
                map = ctx.mkStore(map, key, value);
                addDiscoveredKey(vars[i]);
                BoolExpr exists = ctx.mkSetMembership(key, set);
                size = incrementSizeIfNotExists(size, exists);
                set = ctx.mkSetAdd(set, key);
            }
        }
        model.setArray(map);
        model.setSize(size);
        return model.getReference();
    }

    public BoolExpr existsByKeyAndValueCondition(MapModel model, Expr retrieved, Expr key, Expr value) {
        return ctx.mkAnd(
                ctx.mkNot(mapSort.isEmpty(retrieved)),
                ctx.mkEq(mapSort.getKey(retrieved), key),
                ctx.mkEq(mapSort.getValue(retrieved), value)
        );
    }

    private ArrayExpr mkArray(Sort keySort, Sort valueSort) {
        ArraySort arraySort = ctx.mkArraySort(keySort, valueSort);
        return (ArrayExpr) ctx.mkFreshConst("Map", arraySort);
    }

    private ArrayExpr mkEmptyArray(Sort keySort, Expr sentinel) {
        return ctx.mkConstArray(keySort, sentinel);
    }

    public Expr size(MapModel model) {
        if (model.isSizeUnknown()) {
            ArrayExpr set = ctx.mkEmptySet(mapSort.getKeySort());
            ArithExpr size = ctx.mkInt(0);
            for (Tuple<Expr> tuple : getDiscoverableKeys()) {
                Expr key = tuple.getO2();
                BoolExpr exists = existsByKeyCondition(model, ctx.mkSelect(model.getArray(), key), key);
                BoolExpr isNotInSet = ctx.mkNot(ctx.mkSetMembership(key, set));
                size = (ArithExpr) ctx.mkITE(ctx.mkAnd(exists, isNotInSet),
                        ctx.mkAdd(size, ctx.mkInt(1)), size);
                set = ctx.mkSetAdd(set, key);
            }

            // save condition in state if not present
            Expr condition = ctx.mkGe(model.getSize(), size);
            if (!ctx.containsAssertion(condition))
                solver.add(ctx.mkGe(model.getSize(), size));
        }

        return model.getSize();
    }

    private Expr put(Expr var1, Expr key, Expr value, boolean shouldBeAbsent) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr keyWrapped = sortUnion.wrapValue(key);

        Expr retrieved = ctx.mkSelect(map, keyWrapped);
        BoolExpr exists = existsByKeyCondition(model, retrieved, keyWrapped);
        Expr previous = ctx.mkITE(exists, retrieved, sentinel);

        value = sortUnion.wrapValue(value);
        if (shouldBeAbsent)
            value = ctx.mkITE(exists, mapSort.getValue(previous), value);

        Expr newValue = mapSort.mkDecl(keyWrapped, value, ctx.mkBool(false));
        ArrayExpr newMap = ctx.mkStore(map, keyWrapped, newValue);

        ArithExpr size = (ArithExpr) ctx.mkITE(exists,
                model.getSize(), ctx.mkAdd(model.getSize(), ctx.mkInt(1)));

        model.setArray(newMap);
        model.setSize(size);
        addDiscoveredKey(key);

        Expr sentinelValue = mapSort.getValue(sentinel);
        return sortUnion.unwrapValue(mapSort.getValue(previous), sentinelValue);
    }

    private void putAllTKSU(MapModel target, MapModel source) {
        ArrayExpr map = source.getArray();
        ArithExpr size = source.getSize();
        for (Tuple<Expr> tuple : getDiscoverableKeys()) {
            Expr key = tuple.getO2();

            Expr retrievedFromSource = ctx.mkSelect(map, key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);

            Expr retrievedFromTarget = ctx.mkSelect(target.getArray(), key);
            BoolExpr existsInTarget = existsByKeyCondition(target, retrievedFromTarget, key);

            Expr newValue = mapSort.mkDecl(key, mapSort.getValue(retrievedFromTarget), ctx.mkBool(false));
            BoolExpr shouldSave = ctx.mkAnd(existsInTarget, ctx.mkNot(existsInSource));

            map = (ArrayExpr) ctx.mkITE(shouldSave, ctx.mkStore(map, key, newValue), map);
            size = (ArithExpr) ctx.mkITE(shouldSave, ctx.mkAdd(size, ctx.mkInt(1)), size);
        }

        target.setArray(map);
        target.setSizeUnknown(true);
        target.setSize(size);
    }

    private void putAllTUSU(MapModel target, MapModel source) {
        ArrayExpr map = target.getArray();
        for (Tuple<Expr> tuple : getDiscoverableKeys()) {
            Expr key = tuple.getO2();
            Expr retrievedFromSource = ctx.mkSelect(source.getArray(), key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);
            Expr newValue = mapSort.mkDecl(key, mapSort.getValue(retrievedFromSource), ctx.mkBool(false));
            map = (ArrayExpr) ctx.mkITE(existsInSource,
                    ctx.mkStore(map, key, newValue), map);
        }

        ArithExpr size = ctx.mkAdd(target.getSize(), source.getSize()); // TODO: is this correct?

        target.setArray(map);
        target.setSize(size);
    }

    private void putAllTKSK(MapModel target, MapModel source) {
        ArrayExpr map = target.getArray();
        ArithExpr size = target.getSize();
        for (Tuple<Expr> tuple : getDiscoverableKeys()) {
            Expr key = tuple.getO2();

            Expr retrievedFromSource = ctx.mkSelect(source.getArray(), key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);

            Expr retrievedFromTarget = ctx.mkSelect(map, key);
            BoolExpr existsInTarget = existsByKeyCondition(target, retrievedFromTarget, key);

            Expr newValue = mapSort.mkDecl(key, mapSort.getValue(retrievedFromSource), ctx.mkBool(false));
            map = (ArrayExpr) ctx.mkITE(existsInSource,
                    ctx.mkStore(map, key, newValue), map);

            size = (ArithExpr) ctx.mkITE(ctx.mkAnd(existsInSource, ctx.mkNot(existsInTarget)),
                    ctx.mkAdd(size, ctx.mkInt(1)), size);
        }

        target.setArray(map);
        target.setSize(size);
    }

    private Expr getByKey(MapModel model, Expr key) {
        addDiscoveredKey(key);
        Expr keyWrapped = sortUnion.wrapValue(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), keyWrapped);
        BoolExpr exists = existsByKeyCondition(model, retrieved, keyWrapped);
        return ctx.mkITE(exists, retrieved, sentinel);
    }

    private BoolExpr existsByKeyCondition(MapModel model, Expr retrieved, Expr key) {
        return ctx.mkAnd(
                ctx.mkNot(mapSort.isEmpty(retrieved)),
                ctx.mkEq(mapSort.getKey(retrieved), key)
        );
    }

    private ArithExpr incrementSizeIfNotExists(ArithExpr size, BoolExpr exists) {
        return (ArithExpr) ctx.mkITE(exists, size, ctx.mkAdd(size, ctx.mkInt(1)));
    }

    private MapModel copyModel(MapModel model) {
        MapModel newModel = new MapModel(model);
        stack.add(newModel.getReference(), newModel);
        return newModel;
    }

    private MapModel getModel(Expr var1) {
        return getModel(var1, true);
    }

    private MapModel getModel(Expr expr, boolean isSizeUnknown) {
        if (stack.get(expr).isEmpty())
            return createModel(expr, isSizeUnknown);
        return stack.get(expr).orElseThrow();
    }

    private MapModel createModel(Expr expr, boolean isSizeUnknown) {
        MapModel mapModel = constructor(expr, isSizeUnknown);
        stack.add(expr, mapModel);
        return mapModel;
    }

// gremlin niko
    private void addDiscoveredKey(Expr key) {
        if (!containsDiscoveredKey(key)) {
            Tuple<Expr> tuple = new Tuple<>(key, sortUnion.wrapValue(key));
            this.discoveredKeys.add(key, tuple);
        }
    }

    private void addUndiscoveredKey(Expr keyWrapped) {
        if (!containsWrappedDiscoveredKey(keyWrapped)) {
            Tuple<Expr> tuple = new Tuple<>(null, keyWrapped);
            this.discoveredKeys.add(keyWrapped, tuple);
        }
    }

    private boolean containsDiscoveredKey(Expr keyUnwrapped) {
        for (Tuple<Expr> tuple : discoveredKeys.getAll()) {
            if (keyUnwrapped.equals(tuple.getO1())) return true;
        }
        return false;
    }

    private boolean containsWrappedDiscoveredKey(Expr keyWrapped) {
        for (Tuple<Expr> tuple : discoveredKeys.getAll()) {
            if (keyWrapped.equals(tuple.getO2())) return true;
        }
        return false;
    }
}