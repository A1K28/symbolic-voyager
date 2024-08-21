package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.Optional;
import java.util.UUID;

import static com.github.a1k28.evoc.core.z3extended.Z3Helper.*;

public class Z3MapCollection implements IStack {
    private final Context ctx;
    private final Z3Stack<Integer, MapModel> stack;

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public Z3MapCollection(Context context) {
        this.ctx = context;
        this.stack = new Z3Stack<>();
    }

    public Expr constructor(Expr var1) {
        return constructor(ihc(var1), ctx.mkInt(0));
    }

    public Optional<MapModel> getMap(Expr var1) {
        return stack.get(ihc(var1));
    }

    private ArrayExpr constructor(int hashCode, ArithExpr size) {
        TupleSort sort = mkMapSort(ctx, ctx.mkStringSort(), ctx.mkStringSort());
        ArrayExpr array = arrayConstructor(hashCode);

        MapModel mapModel = new MapModel(
                array, size, sort, mkMapSentinel(ctx, ctx.mkStringSort(), ctx.mkStringSort()));
        stack.add(hashCode, mapModel);

        return array;
    }

    private ArrayExpr arrayConstructor(int hashCode) {
//        TupleSort sort = mapSort(ctx, SortType.OBJECT.value(ctx), SortType.OBJECT.value(ctx));
        TupleSort sort = mkMapSort(ctx, ctx.mkStringSort(), ctx.mkStringSort());
        ArraySort arraySort = ctx.mkArraySort(ctx.mkIntSort(), sort);
        return (ArrayExpr) ctx.mkFreshConst("Map"+hashCode, arraySort);
    }

    public Expr get(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        return model.getValue(getEntry(model, key, model.getSentinel(), false));
    }

    public Expr getOrDefault(Expr var1, Expr key, Expr def) {
        MapModel model = getModel(var1);
        return model.getValue(getEntry(model, key, def, false));
    }

    public Expr put(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, ctx.mkBool(false));
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, ctx.mkBool(true));
    }

    public Expr size(Expr var1) {
        MapModel model = getModel(var1);
        return sizeReduced(model);
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr containsKey(Expr var1, Expr key) {
        return contains(var1, key, false);
    }

    public BoolExpr containsValue(Expr var1, Expr value) {
        return contains(var1, value, true);
    }

    public Expr remove(Expr var1, Expr key) {
        MapModel model = getModel(var1);

        // find element
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), false), key)
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, body);
        Z3Translator.makeSolver().add(constraint);

        Expr previousValue = ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                model.getSentinel());

        // replace array
        ArrayExpr array = model.getArray();
        array = (ArrayExpr) ctx.mkITE(exists,
                ctx.mkStore(array, i, model.getSentinel()),
                array);
        model.setArray(array);

        return model.getValue(previousValue);
    }

    public BoolExpr removeByKeyAndValue(Expr var1, Expr key, Expr value) {
        MapModel model = getModel(var1);

        // find element
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key),
                ctx.mkEq(model.getValue(ctx.mkSelect(model.getArray(), i)), value)
        );
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        // replace array
        ArrayExpr array = model.getArray();
        array = (ArrayExpr) ctx.mkITE(exists,
                ctx.mkStore(array, i, model.getSentinel()),
                array);
        model.setArray(array);

        return exists;
    }

    public Expr putAll(Expr var1, Expr var2) {
        MapModel target = getModel(var1);
        MapModel source = getModel(var2);

        IntExpr i = ctx.mkIntConst("i");

        // Create a quantified formula to count matches
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, target.getSize()),
                ctx.mkNot(source.isEmpty(ctx.mkSelect(source.getArray(), i)))
        );

        BoolExpr quantifier = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(
                        body,
                        put(target,
                                source.getKey(ctx.mkSelect(source.getArray(), i)),
                                source.getValue(ctx.mkSelect(source.getArray(), i)),
                                ctx.mkBool(false)
                        )
                ),
                1, null, null, null, null
        );

        // TODO: add to solver?
        return null;
    }

    public Expr clear(Expr var1) {
        MapModel model = getModel(var1);

        ArrayExpr updatedArray = arrayConstructor(ihc(var1));

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());

        BoolExpr condition = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize())
        );

        BoolExpr action = ctx.mkEq(
                ctx.mkSelect(updatedArray, i),
                model.getSentinel()
        );

        BoolExpr rule = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(condition, action),
                1,
                null,           // quantifier ID
                null,           // skolem ID
                null,           // patterns
                null            // no-patterns
        );

        Z3Translator.makeSolver().add(rule);

        model.setArray(updatedArray);
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        MapModel m1 = getModel(var1);
        MapModel m2 = getModel(var2);

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());

        Expr e1 = ctx.mkSelect(m1.getArray(), i);
        Expr e2 = getEntry(m2,
                m1.getKey(e1),
                m2.getSentinel(),
                false);

        BoolExpr isEmpty1 = m1.isEmpty(e1);
        BoolExpr isEmpty2 = m2.isEmpty(e2);

        BoolExpr bothExistCondition = ctx.mkAnd(ctx.mkNot(isEmpty1), ctx.mkNot(isEmpty2));
        BoolExpr bothAreEmptyCondition = ctx.mkAnd(isEmpty1, isEmpty2);
        BoolExpr eqCondition = ctx.mkOr(bothAreEmptyCondition,
                ctx.mkAnd(bothExistCondition, ctx.mkEq(m2.getValue(e2), m1.getValue(e1))));

        BoolExpr indexCondition = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, m1.getSize())
        );

        BoolExpr sizesMatch = ctx.mkEq(sizeReduced(m1), sizeReduced(m2));
        BoolExpr allMatch = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(
                        indexCondition,
                        eqCondition),
                1,
                null,           // quantifier ID
                null,           // skolem ID
                null,           // patterns
                null            // no-patterns
        );

        return ctx.mkAnd(sizesMatch, allMatch);
    }

    public BoolExpr replaceByKeyAndValue(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        MapModel model = getModel(var1);

        // find element
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key),
                ctx.mkEq(model.getValue(ctx.mkSelect(model.getArray(), i)), oldValue)
        );
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        // replace array
        ArrayExpr array = model.getArray();
        array = (ArrayExpr) ctx.mkITE(exists,
                ctx.mkStore(array, i, newValue),
                array);
        model.setArray(array);

        return exists;
    }

    public Expr replace(Expr var1, Expr key, Expr value) {
        MapModel model = getModel(var1);

        // find element
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), false), key)
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, body);
        Z3Translator.makeSolver().add(constraint);

        Expr previousValue = ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                model.getSentinel());

        // replace array
        ArrayExpr array = model.getArray();
        array = (ArrayExpr) ctx.mkITE(exists,
                ctx.mkStore(array, i, value),
                array);
        model.setArray(array);

        return model.getValue(previousValue);
    }

    public Expr copyOf(Expr var1) {
        MapModel model = getModel(ihc(var1), ctx.mkInt(0));
        MapModel newModel = getModel(UUID.randomUUID().toString().hashCode(), model.getSize());

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());

        BoolExpr condition = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize())
        );
        BoolExpr action = ctx.mkEq(
                ctx.mkSelect(model.getArray(), i),
                ctx.mkSelect(newModel.getArray(), i)
        );

        BoolExpr rule = ctx.mkForall(
                new Expr[]{i},
                ctx.mkImplies(condition, action),
                1,
                null,           // quantifier ID
                null,           // skolem ID
                null,           // patterns
                null            // no-patterns
        );

        Z3Translator.makeSolver().add(rule);

        return newModel.getArray();
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(UUID.randomUUID().toString().hashCode(), ctx.mkInt(0));
        for (int i = 0; i < vars.length; i+=2)
            put(model, vars[i], vars[i+1], ctx.mkBool(false));
        return model.getArray();
    }

    private Expr getEntry(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), valueComparison), expr)
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, body);
        Z3Translator.makeSolver().add(constraint);

        return ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                defaultValue);
    }

    private BoolExpr contains(Expr var1, Expr expr, boolean valueComparison) {
        MapModel model = getModel(var1);

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), valueComparison), expr)
        );

        return ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);
    }

    private Expr put(Expr var1, Expr key, Expr value, BoolExpr shouldBeAbsent) {
        MapModel model = getModel(var1);
        return put(model, key, value, shouldBeAbsent);
    }

    private Expr put(MapModel model, Expr key, Expr value, BoolExpr shouldBeAbsent) {
        // check for existence (same as getEntry method)
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), false), key)
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, body);
        Z3Translator.makeSolver().add(constraint);

        Expr previousValue = ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                model.getSentinel());

        BoolExpr isAbsent = ctx.mkNot(exists);
        BoolExpr absenceSatisfiability = (BoolExpr) ctx.mkITE(shouldBeAbsent,
                isAbsent, ctx.mkBool(true));

        // replace element
        ArrayExpr array = model.getArray();

        i = (IntExpr) ctx.mkITE(isAbsent, model.getSize(), i);
        array = (ArrayExpr) ctx.mkITE(absenceSatisfiability,
                ctx.mkStore(array, i, model.mkDecl(key, value, ctx.mkBool(false))),
                array);

        ArithExpr newSize = (ArithExpr) ctx.mkITE(isAbsent,
                increment(ctx, model.getSize()),
                model.getSize());

        model.setArray(array);
        model.setSize(newSize);

        return model.getValue(previousValue);
    }

    private Expr getKeyOrValue(MapModel model, Expr expr, boolean valueComparison) {
        return valueComparison ? model.getValue(expr) : model.getKey(expr);
    }

    private ArithExpr sizeReduced(MapModel model) {
        ArrayExpr array = model.getArray();
        ArithExpr size = model.getSize();

        IntExpr count = ctx.mkIntConst("count");
        IntExpr index = ctx.mkIntConst("index");

        // Create a quantified formula to count matches
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), index),
                ctx.mkLt(index, size),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(array, index)))
        );

        BoolExpr quantifier = ctx.mkForall(
                new Expr[]{index},
                ctx.mkITE(body,
                        ctx.mkEq(count, ctx.mkAdd(count, ctx.mkInt(1))),
                        ctx.mkEq(count, count)),
                1, null, null, null, null
        );

        // Create assertions
        BoolExpr assertions = ctx.mkAnd(
                ctx.mkEq(count, ctx.mkInt(0)),  // Initialize count to 0
                quantifier
        );

        return (ArithExpr) ctx.mkITE(assertions, count, ctx.mkInt(0));
    }

    private MapModel getModel(Expr var1) {
        ArithExpr size = (ArithExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
        return getModel(ihc(var1), size);
    }

    private MapModel getModel(int hashCode, ArithExpr size) {
        if (stack.get(hashCode).isEmpty()) {
            constructor(hashCode, size);
        }
        return stack.get(hashCode).orElseThrow();
    }

    public static int ihc(Object o) {
        if (o instanceof Expr && o.toString().contains("Map")) {
            try {
                return Integer.parseInt(o.toString().replace("Map", ""));
            } catch (NumberFormatException ignored) {}
        }
        return System.identityHashCode(o);
    }
}
