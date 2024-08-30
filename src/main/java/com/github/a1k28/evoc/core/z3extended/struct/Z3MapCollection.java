package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3SortState;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.Optional;
import java.util.UUID;

public class Z3MapCollection implements IStack {
    private final Context ctx;
    private final Z3SortState sortState;
    private final Z3Stack<Integer, MapModel> stack;

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public Z3MapCollection(Context context, Z3SortState sortState) {
        this.ctx = context;
        this.sortState = sortState;
        this.stack = new Z3Stack<>();
    }

    public Expr constructor(Expr var1) {
        return constructor(ihc(var1), false);
    }

    public Optional<MapModel> getMap(Expr var1) {
        return stack.get(ihc(var1));
    }

    public Optional<MapModel> getInitialMap(Expr var1) {
        return stack.getFirst(ihc(var1));
    }

    private ArrayExpr constructor(int hashCode, boolean isSizeUnknown) {
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
        } else {
            size = ctx.mkInt(0);
            array = mkEmptyArray(keySort, sentinel);
        }

        // size assertion
        BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
        Z3Translator.makeSolver().add(sizeAssertion);

        MapModel mapModel = new MapModel(
                hashCode, array, size, isSizeUnknown, sort, sentinel);
        stack.add(hashCode, mapModel);

        return array;
    }

    private ArrayExpr mkArray(int hashCode, Sort keySort, TupleSort valueSort) {
        ArraySort arraySort = ctx.mkArraySort(keySort, valueSort);
        return (ArrayExpr) ctx.mkFreshConst("Map"+hashCode, arraySort);
    }

    private ArrayExpr mkEmptyArray(Sort keySort, Expr sentinel) {
        return ctx.mkConstArray(keySort, sentinel);
    }

    public Expr get(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        return model.getValue(getEntry(model, key, model.getSentinel(), false));
    }

    public Expr getEntry(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        return getEntry(model, key, model.getSentinel(), false);
    }

    public Expr getOrDefault(Expr var1, Expr key, Expr def) {
        MapModel model = getModel(var1);
        return model.getValue(getEntry(model, key, def, false));
    }

    public Expr put(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, false);
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, true);
    }

    public Expr size(Expr var1) {
        MapModel model = getModel(var1);
        return model.getSize();
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr containsKey(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        model.addDiscoveredKey(key);
        return exists;
    }

    public BoolExpr containsValue(Expr var1, Expr value) {
        MapModel model = getModel(var1);
        if (model.isSizeUnknown()) {
            Expr key = ctx.mkFreshConst("key", model.getKeySort());
            Expr retrieved = ctx.mkSelect(model.getArray(), key);
            BoolExpr exists = existsByKeyAndValueCondition(model, retrieved, key, value);

            BoolExpr existsMatch = ctx.mkExists(
                    new Expr[]{key},
                    exists,
                    1, null, null, null, null
            );
            Z3Translator.makeSolver().add(ctx.mkImplies(existsMatch, exists));

            model.addDiscoveredKey(key);
            return existsMatch;
        } else {
            BoolExpr exists = ctx.mkBool(false);
            for (Expr key : model.getDiscoveredKeys()) {
                Expr retrieved = ctx.mkSelect(model.getArray(), key);
                exists = ctx.mkOr(exists, existsByKeyAndValueCondition(model, retrieved, key, value));
            }
            return exists;
        }
    }

    public Expr remove(Expr var1, Expr key) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());
        Expr previousValue = model.getValue(previous);

        Expr value = model.mkDecl(key, previousValue, ctx.mkBool(true));
        map = ctx.mkStore(map, key, value);

//        if (model.isSizeUnknown())
        model.setSize(decrementSizeIfExists(model.getSize(), exists));

        model.addDiscoveredKey(key);
        model.setArray(map);

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

    // TODO: fix for unknown size maps
    public Expr putAll(Expr var1, Expr var2) {
        MapModel target = getModel(var1);
        MapModel source = getModel(var2);

        ArrayExpr map = target.getArray();
        for (Expr key : source.getDiscoveredKeys()) {
            Expr value = getByKey(source, key);
            map = ctx.mkStore(map, key, value);
        }

        source.setArray(map);
        source.setSize(ctx.mkAdd(target.getSize(), source.getSize()));
        return null;
    }

    public Expr clear(Expr var1) {
        MapModel model = getModel(var1);
        model = copyModel(model);
        ArrayExpr updatedArray = mkEmptyArray(model.getKeySort(), model.getSentinel());
        model.setArray(updatedArray);
        model.setSize(ctx.mkInt(0));
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        MapModel m1 = getModel(var1);
        MapModel m2 = getModel(var2);

        // Create a symbolic index
        Expr index = ctx.mkFreshConst("index", m1.getKeySort());

        // Assert that for all indices, the values in both arrays are equal
        return ctx.mkForall(
                new Expr[]{index},
                equalsValues(m1, m2, index),
                1, null, null, null, null
        );
    }

    public BoolExpr replaceByKeyAndValue(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        MapModel model = copyModel(getModel(var1));
        ArrayExpr map = model.getArray();

        Expr previousValue = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValueCondition(model, previousValue, key, oldValue);

        Expr value = model.mkDecl(key, newValue, ctx.mkBool(false));
        value = ctx.mkITE(exists, value, previousValue);

        map = ctx.mkStore(map, key, value);

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
        MapModel model = getModel(ihc(var1), false);
        MapModel newModel = new MapModel(model);
        newModel.setHashCode(UUID.randomUUID().toString().hashCode());
        stack.add(newModel.getHashCode(), newModel);
        return newModel.getArray();
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(UUID.randomUUID().toString().hashCode(), false);
        ArrayExpr map = model.getArray();
        ArrayExpr set = ctx.mkEmptySet(model.getKeySort());
        ArithExpr size = ctx.mkInt(0);
        for (int i = 0; i < vars.length; i+=2) {
            Expr value = model.mkDecl(vars[i], vars[i+1], ctx.mkBool(false));
            map = ctx.mkStore(map, vars[i], value);
            model.addDiscoveredKey(vars[i]);
            BoolExpr exists = ctx.mkSetMembership(vars[i], set);
            size = incrementSizeIfNotExists(size, exists);
        }
        model.setArray(map);
        model.setSize(size);
        return model.getArray();
    }

    private Expr getEntry(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        Expr retrieved = valueComparison ?
                getByValue(model, expr) : getByKey(model, expr);
        BoolExpr isEmpty = model.isEmpty(retrieved);
        return ctx.mkITE(isEmpty, defaultValue, retrieved);
    }

    private Expr put(Expr var1, Expr key, Expr value, boolean shouldBeAbsent) {
        MapModel model = copyModel(getModel(var1));
        return put(model, key, value, shouldBeAbsent);
    }

    private Expr put(MapModel model, Expr key, Expr value, boolean shouldBeAbsent) {
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKeyCondition(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());

        if (shouldBeAbsent)
            value = ctx.mkITE(exists, model.getValue(previous), value);

        Expr newValue = model.mkDecl(key, value, ctx.mkBool(false));
        ArrayExpr newMap = ctx.mkStore(map, key, newValue);

        model.setArray(newMap);
        model.addDiscoveredKey(key);
        model.setSize(incrementSizeIfNotExists(model.getSize(), exists));

        return model.getValue(previous);
    }

    private BoolExpr equalsValues(MapModel m1, MapModel m2, Expr index) {
        // either they are both empty or all values match
        Expr v1 = ctx.mkSelect(m1.getArray(), index);
        Expr v2 = ctx.mkSelect(m2.getArray(), index);
        return ctx.mkOr(
                ctx.mkAnd(
                        m1.isEmpty(v1),
                        m2.isEmpty(v2)
                ),
                ctx.mkAnd(
                        ctx.mkEq(m1.getKey(v1), m2.getKey(v2)),
                        ctx.mkEq(m1.getValue(v1), m2.getValue(v2)),
                        ctx.mkEq(m1.isEmpty(v1), m2.isEmpty(v2))
                )
        );
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

    private BoolExpr existsByKeyAndValueCondition(MapModel model, Expr retrieved, Expr key, Expr value) {
        return ctx.mkAnd(
                ctx.mkNot(model.isEmpty(retrieved)),
                ctx.mkEq(model.getKey(retrieved), key),
                ctx.mkEq(model.getValue(retrieved), value)
        );
    }

    private ArithExpr incrementSizeIfNotExists(ArithExpr size, BoolExpr exists) {
        return (ArithExpr) ctx.mkITE(exists, size, ctx.mkAdd(size, ctx.mkInt(1)));
    }

    private ArithExpr decrementSizeIfExists(ArithExpr size, BoolExpr exists) {
        return (ArithExpr) ctx.mkITE(exists, ctx.mkSub(size, ctx.mkInt(1)), size);
    }

    private MapModel copyModel(MapModel model) {
        MapModel newModel = new MapModel(model);
        stack.add(newModel.getHashCode(), newModel);
        return newModel;
    }

    private MapModel getModel(Expr var1) {
        return getModel(ihc(var1), true);
    }

    private MapModel getModel(int hashCode, boolean isSizeUnknown) {
        if (stack.get(hashCode).isEmpty()) {
            constructor(hashCode, isSizeUnknown);
        }
        return stack.get(hashCode).orElseThrow();
    }
// gremlin niko
    public static int ihc(Object o) {
        if (o instanceof Expr && o.toString().startsWith("Map")) {
            try {
                return Integer.parseInt(o.toString().replace("Map", ""));
            } catch (NumberFormatException ignored) {}
        }
        return System.identityHashCode(o);
    }
}
