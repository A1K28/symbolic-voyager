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

    public Optional<MapModel> getMapFirst(Expr var1) {
        return stack.getFirst(ihc(var1));
    }

    private ArrayExpr constructor(int hashCode, boolean isSizeUnknown) {
        ArithExpr size = isSizeUnknown ?
                (ArithExpr) ctx.mkFreshConst("size", ctx.mkIntSort()) : ctx.mkInt(0);

        // TODO: handle sorts?
        Sort keySort = ctx.mkStringSort();
        Sort valueSort = ctx.mkStringSort();

        TupleSort sort = sortState.mkMapSort(ctx, keySort, valueSort);
        ArrayExpr array = arrayConstructor(hashCode, keySort, valueSort);

        MapModel mapModel = new MapModel(
                hashCode, array, size, isSizeUnknown, sort,
                sortState.mkMapSentinel(ctx, ctx.mkStringSort(), ctx.mkStringSort()));
        stack.add(hashCode, mapModel);

        // size assertion
        BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
        Z3Translator.makeSolver().add(sizeAssertion);

        // element assertion
        if (!isSizeUnknown) clear(mapModel, array);

        return array;
    }

    private ArrayExpr arrayConstructor(int hashCode, Sort keySort, Sort valueSort) {
        TupleSort sort = sortState.mkMapSort(ctx, keySort, valueSort);
        ArraySort arraySort = ctx.mkArraySort(keySort, sort);
        return (ArrayExpr) ctx.mkFreshConst("Map"+hashCode, arraySort);
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

        if (model.isSizeUnknown())
            return model.getSize();

        // TODO: store size to reduce computation?
        IntExpr size = ctx.mkInt(0);
        ArrayExpr emptySet = ctx.mkEmptySet(model.getKeySort());
        for (Expr key : model.getDiscoveredKeys()) {
            Expr retrieved = ctx.mkSelect(model.getArray(), key);
            BoolExpr exists = existsByKey(model, retrieved, key);
            BoolExpr isAccounted = ctx.mkSetMembership(key, emptySet);
            size = (IntExpr) ctx.mkITE(ctx.mkAnd(ctx.mkNot(isAccounted), exists),
                    ctx.mkAdd(size, ctx.mkInt(1)), size);
            emptySet = ctx.mkSetAdd(emptySet, key);
        }
        return size;
    }

    public Expr unresolvedSize(Expr var1) {
//        MapModel model = getModel(var1);

        MapModel model = stack.getFirst(ihc(var1)).orElseThrow();
        return model.getSize();

//        IntExpr size = ctx.mkInt(0);
//        ArrayExpr emptySet = ctx.mkEmptySet(model.getKeySort());
//        for (Expr key : model.getKeys()) {
//            Expr retrieved = ctx.mkSelect(model.getArray(), key);
//            BoolExpr exists = existsByKey(model, retrieved, key);
//            BoolExpr isAccounted = ctx.mkSetMembership(key, emptySet);
//            size = (IntExpr) ctx.mkITE(ctx.mkAnd(ctx.mkNot(isAccounted), exists),
//                    ctx.mkAdd(size, ctx.mkInt(1)), size);
//            emptySet = ctx.mkSetAdd(emptySet, key);
//        }
////        return size;
//        Z3Translator.makeSolver().check();
//        System.out.println("SOLVED: " + Z3Translator.makeSolver().getModel().eval(ctx.mkAdd(size, model.getSize()), true));
////        System.out.println("SOLVED: " + Z3Translator.makeSolver().minimize(ctx.mkAdd(size, model.getSize())));
//        return ctx.mkAdd(size, model.getSize());
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr containsKey(Expr var1, Expr key) {
        return contains(getModel(var1), key, false);
    }

    public BoolExpr containsValue(Expr var1, Expr value) {
        return contains(getModel(var1), value, true);
    }

    public Expr remove(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKey(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());
        Expr previousValue = model.getValue(previous);

        Expr value = model.mkDecl(key, previousValue, ctx.mkBool(true));
        map = ctx.mkStore(map, key, value);
        model.setArray(map);

        return previousValue;
    }

    public BoolExpr removeByKeyAndValue(Expr var1, Expr key, Expr value) {
        MapModel model = getModel(var1);
        ArrayExpr map = model.getArray();

        Expr previousValue = getByKeyAndValue(model, key, value);
        Expr val = model.mkDecl(key, value, ctx.mkBool(true));
        BoolExpr isEmpty = model.isEmpty(previousValue);
        val = ctx.mkITE(isEmpty, model.getSentinel(), val);

        map = ctx.mkStore(map, key, val);
        model.setArray(map);

        return ctx.mkNot(isEmpty);
    }

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
        ArrayExpr updatedArray = arrayConstructor(
                model.getHashCode(), model.getKeySort(), model.getValueSort());
        clear(model, updatedArray);
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
        MapModel model = getModel(var1);
        ArrayExpr map = model.getArray();

        Expr previousValue = getByKeyAndValue(model, key, oldValue);
        Expr val = model.mkDecl(key, newValue, ctx.mkBool(false));
        BoolExpr isEmpty = model.isEmpty(previousValue);
        val = ctx.mkITE(isEmpty, model.getSentinel(), val);

        map = ctx.mkStore(map, key, val);
        model.setArray(map);

        return ctx.mkNot(isEmpty);
    }

    public Expr replace(Expr var1, Expr key, Expr value) {
        MapModel model = getModel(var1);
        ArrayExpr map = model.getArray();

        Expr previousValue = getByKey(model, key);
        Expr newValue = model.mkDecl(key, value, ctx.mkBool(false));

        map = ctx.mkStore(map, key, newValue);
        model.setArray(map);

        return model.getValue(previousValue);
    }

    public Expr copyOf(Expr var1) {
        MapModel model = getModel(ihc(var1), false);
        MapModel newModel = new MapModel(model);
        newModel.setHashCode(UUID.randomUUID().toString().hashCode());;
        stack.add(newModel.getHashCode(), newModel);
        return newModel.getArray();
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(UUID.randomUUID().toString().hashCode(), false);
        ArrayExpr map = model.getArray();
        for (int i = 0; i < vars.length; i+=2) {
            Expr value = model.mkDecl(vars[i], vars[i+1], ctx.mkBool(false));
            map = ctx.mkStore(map, vars[i], value);
            model.addDiscoveredKey(vars[i]);
        }
        model.setArray(map);
        return model.getArray();
    }

    private Expr clear(MapModel model, ArrayExpr array) {
        // Create a symbolic index
        Expr index = ctx.mkFreshConst("index", model.getKeySort());

        // Assert that for all indices, the values in both arrays are equal
        BoolExpr rule = ctx.mkForall(
                new Expr[]{index},
                model.isEmpty(ctx.mkSelect(array, index)),
                1, null, null, null, null
        );

        Z3Translator.makeSolver().add(rule);

        model.setArray(array);
        return null;
    }

    private BoolExpr contains(MapModel model, Expr expr, boolean valueComparison) {
        Expr value = valueComparison ?
                getByValue(model, expr) : getByKey(model, expr);
        BoolExpr exists = valueComparison ?
                existsByValue(model, expr) : existsByKey(model, expr);
//        BoolExpr isPresent = ctx.mkNot(model.isEmpty(value));

        model.addDiscoveredKey(
                valueComparison ? model.getKey(value) : expr
        );

        return exists;
    }

    private Expr getEntry(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        Expr retrieved = valueComparison ?
                getByValue(model, expr) : getByKey(model, expr);
        BoolExpr isEmpty = model.isEmpty(retrieved);
        return ctx.mkITE(isEmpty, defaultValue, retrieved);
    }

    private Expr put(Expr var1, Expr key, Expr value, boolean shouldBeAbsent) {
        MapModel model = getModel(var1);
        return put(model, key, value, shouldBeAbsent);
    }

    private Expr put(MapModel model, Expr key, Expr value, boolean shouldBeAbsent) {
        model = copyModel(model);
        model.addDiscoveredKey(key);

        ArrayExpr map = model.getArray();

        Expr retrieved = ctx.mkSelect(map, key);
        BoolExpr exists = existsByKey(model, retrieved, key);
        Expr previous = ctx.mkITE(exists, retrieved, model.getSentinel());

        if (shouldBeAbsent)
            value = ctx.mkITE(exists, model.getValue(previous), value);

        Expr newValue = model.mkDecl(key, value, ctx.mkBool(false));
        ArrayExpr newMap = ctx.mkStore(map, key, newValue);

        if (model.isSizeUnknown())
            model.setSize(incrementSizeIfExists(model.getSize(), exists));
        model.setArray(newMap);

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

    private BoolExpr existsByKey(MapModel model, Expr key) {
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        return existsByKey(model, retrieved, key);
    }

    private BoolExpr existsByValue(MapModel model, Expr value) {
        Expr key = ctx.mkFreshConst("unknown", model.getKeySort());
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        return existsByKeyAndValue(model, retrieved, key, value);
    }

    private Expr getByKey(MapModel model, Expr key) {
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKey(model, retrieved, key);
        return ctx.mkITE(exists, retrieved, model.getSentinel());
    }

    private Expr getByValue(MapModel model, Expr value) {
        Expr key = ctx.mkFreshConst("unknown", model.getKeySort());
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValue(model, retrieved, key, value);
        return ctx.mkITE(exists, retrieved, model.getSentinel());
    }

    private Expr getByKeyAndValue(MapModel model, Expr key, Expr value) {
        model.addDiscoveredKey(key);
        Expr retrieved = ctx.mkSelect(model.getArray(), key);
        BoolExpr exists = existsByKeyAndValue(model, retrieved, key, value);
        return ctx.mkITE(exists, retrieved, model.getSentinel());
    }

    private BoolExpr existsByKey(MapModel model, Expr retrieved, Expr key) {
        return ctx.mkAnd(
                ctx.mkNot(model.isEmpty(retrieved)),
                ctx.mkEq(model.getKey(retrieved), key)
        );
    }

    private BoolExpr existsByKeyAndValue(MapModel model, Expr retrieved, Expr key, Expr value) {
        return ctx.mkAnd(
                ctx.mkNot(model.isEmpty(retrieved)),
                ctx.mkEq(model.getKey(retrieved), key),
                ctx.mkEq(model.getValue(retrieved), value)
        );
    }

    private ArithExpr incrementSizeIfExists(ArithExpr size, BoolExpr exists) {
        return (ArithExpr) ctx.mkITE(exists, size, ctx.mkAdd(size, ctx.mkInt(1)));
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
