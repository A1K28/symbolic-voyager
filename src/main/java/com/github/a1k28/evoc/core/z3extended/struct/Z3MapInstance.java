package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Z3MapInstance implements IStack {
    private final Context ctx;
    private final Z3CachingFactory sortState;
    private final Z3Stack<Integer, MapModel> stack;
    private final Z3ExtendedSolver solver;

    public Z3MapInstance(Context context, Z3CachingFactory sortState, Z3ExtendedSolver solver) {
        this.ctx = context;
        this.sortState = sortState;
        this.solver = solver;
        this.stack = new Z3Stack<>();
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public Expr constructor(Expr var1) {
        MapModel mapModel = constructor(ihc(var1), false);
        stack.add(mapModel.getHashCode(), mapModel);
        return ctx.mkString("Map"+mapModel.getHashCode());
    }

    public Expr constructor(Expr var1, Expr var2) {
        return constructor(ihc(var1), getModel(var2));
    }

    private Expr constructor(int hashCode, MapModel fromMap) {
        MapModel newModel = new MapModel(fromMap);
        newModel.setHashCode(hashCode);
        stack.add(newModel.getHashCode(), newModel);
        return ctx.mkString("Map"+newModel.getHashCode());
    }

    private MapModel constructor(int hashCode, boolean isSizeUnknown) {
        // TODO: handle sorts?
        Sort keySort = ctx.mkStringSort();
        Sort valueSort = ctx.mkStringSort();
        TupleSort sort = sortState.mkMapSort(ctx, keySort, valueSort);
        Expr sentinel = sortState.mkMapSentinel(ctx, ctx.mkStringSort(), ctx.mkStringSort());

        ArrayExpr array;
        ArithExpr size;
        if (isSizeUnknown) {
            size = (ArithExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            array = mkArray(hashCode, keySort, sort);

            // size assertion
            BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
            solver.add(sizeAssertion);
        } else {
            size = ctx.mkInt(0);
            array = mkEmptyArray(keySort, sentinel);
        }

        return new MapModel(
                hashCode, array, size, isSizeUnknown, sort, sentinel);
    }

    public Optional<MapModel> getInitialMap(Expr var1) {
        return stack.getFirst(ihc(var1));
    }

    public Expr get(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        return getValue(model, key, model.getValue(model.getSentinel()), false);
    }

    public Expr getOrDefault(Expr var1, Expr key, Expr def) {
        MapModel model = getModel(var1);
        return getValue(model, key, def, false);
    }

    public Expr put(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, false);
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, true);
    }

    public Expr size(Expr var1) {
        MapModel model = getModel(var1);

        if (model.isSizeUnknown() && model.isSizeOutdated()) {
            ArrayExpr set = ctx.mkEmptySet(model.getKeySort());
            ArithExpr size = ctx.mkInt(0);
            for (Expr key : model.getDiscoveredKeys()) {
                BoolExpr exists = existsByKeyCondition(model, ctx.mkSelect(model.getArray(), key), key);
                BoolExpr isNotInSet = ctx.mkNot(ctx.mkSetMembership(key, set));
                size = (ArithExpr) ctx.mkITE(ctx.mkAnd(exists, isNotInSet),
                        ctx.mkAdd(size, ctx.mkInt(1)), size);
                set = ctx.mkSetAdd(set, key);
            }

            // save condition in state if not present
            Expr condition = ctx.mkGe(model.getSize(), size);
            if (!Z3Translator.containsAssertion(condition))
                solver.add(ctx.mkGe(model.getSize(), size));
        }

        return model.getSize();
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr containsKey(Expr var1, Expr key) {
        return containsKey(getModel(var1), key);
    }

    public BoolExpr containsKey(MapModel model, Expr key) {
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        model.addDiscoveredKey(key);
        return exists;
    }

    public BoolExpr containsKeyValuePair(MapModel model, Expr key, Expr value) {
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, key, value);
        return exists;
    }

    public BoolExpr containsValue(Expr var1, Expr value) {
        MapModel model = getModel(var1);
        Expr key = ctx.mkFreshConst("key", model.getKeySort());
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, key, value);

        BoolExpr existsMatch = ctx.mkExists(
                new Expr[]{key},
                exists,
                1, null, null, null, null
        );
        solver.add(ctx.mkImplies(existsMatch, exists));

        model.addDiscoveredKey(key);
        return existsMatch;
    }

    public Expr remove(Expr var1, Expr key) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());
        Expr previousValue = model.getValue(previous);

        map = ctx.mkStore(map, key, model.getSentinel());

        ArithExpr size = (ArithExpr) ctx.mkITE(exists,
                ctx.mkSub(model.getSize(), ctx.mkInt(1)), model.getSize());

        model.setArray(map);
        model.addDiscoveredKey(key);
        model.setSize(size);

        return previousValue;
    }

    public BoolExpr removeByKeyAndValue(Expr var1, Expr key, Expr value) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr previousValue = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, previousValue, key, value);

        Expr newValue = ctx.mkITE(exists, model.getSentinel(), previousValue);

        map = ctx.mkStore(map, key, newValue);

        model.setArray(map);
        model.addDiscoveredKey(key);

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
        ArrayExpr updatedArray = mkEmptyArray(model.getKeySort(), model.getSentinel());
        model.setArray(updatedArray);
        model.setSize(ctx.mkInt(0));
        model.setSizeUnknown(false);
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        MapModel m1 = getModel(var1);
        MapModel m2 = getModel(var2);

        List<Expr> m1DiscoveredKeys = new ArrayList<>(m1.getDiscoveredKeys());
        m2.getDiscoveredKeys().forEach(m1::addDiscoveredKey);
        m1DiscoveredKeys.forEach(m2::addDiscoveredKey);

        return ctx.mkEq(m1.getArray(), m2.getArray());
    }

    public BoolExpr replaceByKeyAndValue(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, key, oldValue);
        Expr value = model.mkDecl(key, newValue, ctx.mkBool(false));
        map = (ArrayExpr) ctx.mkITE(exists, ctx.mkStore(map, key, value), map);

        model.setArray(map);
        model.addDiscoveredKey(key);

        return exists;
    }

    public Expr replace(Expr var1, Expr key, Expr value) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr previousValue = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyCondition(model, previousValue, key);

        Expr newValue = model.mkDecl(key, value, ctx.mkBool(false));
        newValue = ctx.mkITE(exists, newValue, model.getSentinel());

        map = ctx.mkStore(map, key, newValue);

        model.setArray(map);
        model.addDiscoveredKey(key);

        return model.getValue(previousValue);
    }

    public Expr copyOf(Expr var1) {
        MapModel source = getModel(ihc(var1), false);
        MapModel target = new MapModel(source);
        target.setHashCode(UUID.randomUUID().toString().hashCode());
        ArrayExpr arr = mkArray(target.getHashCode(), source.getKeySort(), source.getValueSort());
        target.setArray(arr);
        solver.add(ctx.mkEq(source.getArray(), arr));
        stack.add(target.getHashCode(), target);
        return ctx.mkString("Map"+target.getHashCode());
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(UUID.randomUUID().toString().hashCode(), false);
        ArrayExpr map = model.getArray();
        ArrayExpr set = ctx.mkEmptySet(model.getKeySort());
        ArithExpr size = ctx.mkInt(0);
        if (vars != null) {
            for (int i = 0; i < vars.length; i+=2) {
                Expr value = model.mkDecl(vars[i], vars[i+1], ctx.mkBool(false));
                map = ctx.mkStore(map, vars[i], value);
                model.addDiscoveredKey(vars[i]);
                BoolExpr exists = ctx.mkSetMembership(vars[i], set);
                size = incrementSizeIfNotExists(size, exists);
            }
        }
        model.setArray(map);
        model.setSize(size);
        return ctx.mkString("Map"+model.getHashCode());
    }

    public BoolExpr existsByKeyAndValueCondition(MapModel model, Expr retrieved, Expr key, Expr value) {
        return ctx.mkAnd(
                ctx.mkNot(model.isEmpty(retrieved)),
                ctx.mkEq(model.getKey(retrieved), key),
                ctx.mkEq(model.getValue(retrieved), value)
        );
    }

    private ArrayExpr mkArray(int hashCode, Sort keySort, Sort valueSort) {
        ArraySort arraySort = ctx.mkArraySort(keySort, valueSort);
        return (ArrayExpr) ctx.mkFreshConst("Map"+hashCode, arraySort);
    }

    private ArrayExpr mkEmptyArray(Sort keySort, Expr sentinel) {
        return ctx.mkConstArray(keySort, sentinel);
    }

    private Expr getValue(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        Expr retrieved = valueComparison ?
                getByValue(model, expr) : getByKey(model, expr);
        BoolExpr isEmpty = model.isEmpty(retrieved);
        return ctx.mkITE(isEmpty, defaultValue, model.getValue(retrieved));
    }

    private Expr put(Expr var1, Expr key, Expr value, boolean shouldBeAbsent) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());

        if (shouldBeAbsent)
            value = ctx.mkITE(exists, model.getValue(previous), value);

        Expr newValue = model.mkDecl(key, value, ctx.mkBool(false));
        ArrayExpr newMap = ctx.mkStore(map, key, newValue);

        ArithExpr size = (ArithExpr) ctx.mkITE(exists,
                model.getSize(), ctx.mkAdd(model.getSize(), ctx.mkInt(1)));

        model.setArray(newMap);
        model.addDiscoveredKey(key);
        model.setSize(size);

        return model.getValue(previous);
    }

    private void putAllTKSU(MapModel target, MapModel source) {
        ArrayExpr map = source.getArray();
        ArithExpr size = source.getSize();
        for (Expr key : target.getDiscoveredKeys()) {
            Expr retrievedFromSource = ctx.mkSelect(map, key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);

            Expr retrievedFromTarget = ctx.mkSelect(target.getArray(), key);
            BoolExpr existsInTarget = existsByKeyCondition(target, retrievedFromTarget, key);

            Expr newValue = target.mkDecl(key, target.getValue(retrievedFromTarget), ctx.mkBool(false));
            map = (ArrayExpr) ctx.mkITE(ctx.mkAnd(existsInTarget, ctx.mkNot(existsInSource)),
                    ctx.mkStore(map, key, newValue), map);

            size = (ArithExpr) ctx.mkITE(ctx.mkAnd(existsInTarget, ctx.mkNot(existsInSource)),
                    ctx.mkAdd(size, ctx.mkInt(1)), size);
        }

        target.setArray(map);
        target.setSizeUnknown(true);
        target.setSize(size);
        source.getDiscoveredKeys().forEach(target::addDiscoveredKey);
    }

    private void putAllTUSU(MapModel target, MapModel source) {
        ArrayExpr map = target.getArray();
        for (Expr key : source.getDiscoveredKeys()) {
            Expr retrievedFromSource = ctx.mkSelect(source.getArray(), key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);
            Expr newValue = target.mkDecl(key, source.getValue(retrievedFromSource), ctx.mkBool(false));
            map = (ArrayExpr) ctx.mkITE(existsInSource,
                    ctx.mkStore(map, key, newValue), map);
        }

        ArithExpr size = ctx.mkAdd(target.getSize(), source.getSize()); // TODO: is this correct?

        target.setArray(map);
        target.setSize(size);
        source.getDiscoveredKeys().forEach(target::addDiscoveredKey);
    }

    private void putAllTKSK(MapModel target, MapModel source) {
        ArrayExpr map = target.getArray();
        ArithExpr size = target.getSize();
        for (Expr key : source.getDiscoveredKeys()) {
            Expr retrievedFromSource = ctx.mkSelect(source.getArray(), key);
            BoolExpr existsInSource = existsByKeyCondition(source, retrievedFromSource, key);

            Expr retrievedFromTarget = ctx.mkSelect(map, key);
            BoolExpr existsInTarget = existsByKeyCondition(target, retrievedFromTarget, key);

            Expr newValue = target.mkDecl(key, source.getValue(retrievedFromSource), ctx.mkBool(false));
            map = (ArrayExpr) ctx.mkITE(existsInSource,
                    ctx.mkStore(map, key, newValue), map);

            size = (ArithExpr) ctx.mkITE(ctx.mkAnd(existsInSource, ctx.mkNot(existsInTarget)),
                    ctx.mkAdd(size, ctx.mkInt(1)), size);
        }

        target.setArray(map);
        target.setSize(size);
        source.getDiscoveredKeys().forEach(target::addDiscoveredKey);
    }

    private Expr getByKey(MapModel model, Expr key) {
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        return ctx.mkITE(exists, retrieved, model.getSentinel());
    }

    private Expr getByValue(MapModel model, Expr value) {
        Expr key = ctx.mkFreshConst("unknown", model.getKeySort());
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, key, value);
        return ctx.mkITE(exists, retrieved, model.getSentinel());
    }

    private BoolExpr existsByKeyCondition(MapModel model, Expr retrieved, Expr key) {
        return ctx.mkAnd(
                ctx.mkNot(model.isEmpty(retrieved)),
                ctx.mkEq(model.getKey(retrieved), key)
        );
    }

    private ArithExpr incrementSizeIfNotExists(ArithExpr size, BoolExpr exists) {
        return (ArithExpr) ctx.mkITE(exists, size, ctx.mkAdd(size, ctx.mkInt(1)));
    }

    private MapModel copyModel(MapModel model) {
        MapModel newModel = new MapModel(model);
        newModel.setSize(ctx.mkAdd(model.getSize(), ctx.mkInt(0)));
        stack.add(newModel.getHashCode(), newModel);
        return newModel;
    }

    private MapModel getModel(Expr var1) {
        return getModel(ihc(var1), true);
    }

    private MapModel getModel(int hashCode, boolean isSizeUnknown) {
        if (stack.get(hashCode).isEmpty())
            return createModel(hashCode, isSizeUnknown);
        return stack.get(hashCode).orElseThrow();
    }

    private MapModel createModel(int hashCode, boolean isSizeUnknown) {
        MapModel mapModel = constructor(hashCode, isSizeUnknown);
        stack.add(hashCode, mapModel);
        return mapModel;
    }

// gremlin niko
    public static int ihc(Object o) {
        if (o instanceof Expr) {
            String val = o.toString().replace("\"","");
            if (val.startsWith("Map")) {
                try {
                    val = val.replace("Map", "");
                    int lastIdx = val.indexOf('!');
                    lastIdx = lastIdx == -1 ? val.length() : lastIdx;
                    val = val.substring(0, lastIdx);
                    return Integer.parseInt(val);
                } catch (NumberFormatException ignored) {}
            }
        }
        return System.identityHashCode(o);
    }
}
