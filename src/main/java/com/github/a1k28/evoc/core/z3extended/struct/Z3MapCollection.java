package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.a1k28.evoc.core.z3extended.Z3Helper.*;

public class Z3MapCollection implements IStack {
    private final Context ctx;
    private final Z3Stack<Integer, MapModel> stack;

    @Override
    public void push() {
        stack.push();

//        List<MapModel> all = stack.getAll();
//        for (MapModel model : all) {
//            int hashCode = ihc(model.getArray());
//            constructor(hashCode, model.getSize());
//            MapModel newModel = stack.get(hashCode).orElseThrow();
//            ArrayExpr arrayCopy = ctx.mkStore(
//                    model.getArray(),
//                    ctx.mkInt(0),
//                    ctx.mkSelect(model.getArray(), ctx.mkInt(0)));
//            newModel.setArray(arrayCopy);
//            newModel.setSize(model.getSize());
//        }
    }

    @Override
    public void pop() {
        stack.pop();

//        // pop individual elements from all maps that have gone past the available stack
//        List<MapModel> all = stack.getAll();
//        IntExpr stackLowerBound = ctx.mkInt(stack.getIndex());
//        for (MapModel model : all) {
//            popEntriesFromStack(model, stackLowerBound);
//        }
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
                hashCode, array, size, sort,
                mkMapSentinel(ctx, ctx.mkStringSort(), ctx.mkStringSort()));
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
        return put(var1, key, value, false);
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, true);
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
                                false
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
                null,
                null,
                null,
                null
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
        return copyOf(model, UUID.randomUUID().toString().hashCode()).getArray();
    }

    public Expr of(Expr... vars) {
        MapModel model = getModel(UUID.randomUUID().toString().hashCode(), ctx.mkInt(0));
        for (int i = 0; i < vars.length; i+=2)
            put(model, vars[i], vars[i+1], false);
        return model.getArray();
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

    private MapModel copyOf(MapModel model, int hashCode) {
//        MapModel newModel = getModel(hashCode, model.getSize());
        constructor(hashCode, model.getSize());
        MapModel newModel = stack.get(hashCode).orElseThrow();

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
                1, null, null, null, null
        );

        Z3Translator.makeSolver().add(rule);

        return newModel;
    }

    private Expr getEntry(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
//        IntExpr stackSize = ctx.mkInt(stack.getIndex());

        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), valueComparison), expr)
//                ctx.mkGe(stackSize, model.getStackIndex(ctx.mkSelect(model.getArray(), i)))
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, body);
        Z3Translator.makeSolver().add(constraint);

        return ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                defaultValue);
    }

    public Expr getEntry2(MapModel model, Expr expr, Expr defaultValue, boolean valueComparison) {
        IntExpr i = ctx.mkIntConst("i");
        IntExpr j = ctx.mkIntConst("j");
//        IntExpr stackSize = ctx.mkInt(stack.getIndex());

        // Function to check if an element matches the key
        BoolExpr isMatchI = ctx.mkAnd(
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), valueComparison), expr)
//                ctx.mkGe(stackSize, model.getStackIndex(ctx.mkSelect(model.getArray(), i)))
        );
        BoolExpr isMatchJ = ctx.mkAnd(
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), j))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), j), valueComparison), expr)
//                ctx.mkGe(stackSize, model.getStackIndex(ctx.mkSelect(model.getArray(), j)))
        );

        // Condition: i is the index of the last matching element
        BoolExpr lastMatchCondition = ctx.mkAnd(
                ctx.mkGe(i, ctx.mkInt(0)),
                ctx.mkLt(i, model.getSize()),
                isMatchI,
                ctx.mkForall(new Expr[]{j},
                        ctx.mkImplies(
                                ctx.mkAnd(
                                        ctx.mkGt(j, i),
                                        ctx.mkLt(j, model.getSize())
                                ),
                                ctx.mkNot(isMatchJ)
                        ),
                        1, null, null, null, null
                )
        );

        // If a matching element exists, result is the element at index i
        // Otherwise, result is a default value (you can change this as needed)
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, lastMatchCondition, 1,
                null, null, null, null);

        BoolExpr constraint = ctx.mkImplies(exists, lastMatchCondition);
        Z3Translator.makeSolver().add(constraint);

        return ctx.mkITE(
                exists,
                ctx.mkSelect(model.getArray(), i),
                defaultValue
        );
    }

    private Expr getKeyOrValue(MapModel model, Expr expr, boolean valueComparison) {
        return valueComparison ? model.getValue(expr) : model.getKey(expr);
    }

    private Expr put(Expr var1, Expr key, Expr value, boolean shouldBeAbsent) {
        MapModel model = getModel(var1);
        return put(model, key, value, shouldBeAbsent);
    }

    private Expr put(MapModel model, Expr key, Expr value, boolean shouldBeAbsent) {
        // copy inside stack
        model = copyModel(model);

        // Create a boolean variable to represent whether the index is valid
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
//        BoolExpr exists = (BoolExpr) ctx.mkFreshConst("exists", ctx.mkBoolSort());

        // Create the updated array
        ArrayExpr updatedArray = arrayConstructor(model.getHashCode());

        // If the index is valid, update the array at that index. Otherwise, keep the original array.
//        ArrayExpr updatedArray = (ArrayExpr) ctx.mkITE(exists,
//                ctx.mkStore(model.getArray(), i, model.getSentinel()),
//                model.getArray());

        // Add constraints to define when an index is valid (you may need to adjust this based on your specific requirements)
        BoolExpr existenceCondition = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key)
        );
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, existenceCondition, 1,
                null, null, null, null);

        BoolExpr updateCondition = (BoolExpr) ctx.mkITE(exists,
                ctx.mkEq(updatedArray, ctx.mkStore(model.getArray(), i, model.getSentinel())),
                ctx.mkEq(updatedArray, model.getArray()));

//        ArrayExpr updatedArray = (ArrayExpr) ctx.mkITE(exists,
//                ctx.mkStore(model.getArray(), i, model.getSentinel()),
//                model.getArray());

        updatedArray = ctx.mkStore(updatedArray, model.getSize(),
                model.mkDecl(key, value, ctx.mkBool(false), ctx.mkInt(stack.getIndex())));

        // Add constraints to the solver
        Z3Translator.makeSolver().add(ctx.mkImplies(exists, existenceCondition));
        Z3Translator.makeSolver().add(updateCondition);

        ArithExpr newSize = increment(ctx, model.getSize());

        model.setArray(updatedArray);
        model.setSize(newSize);

        return model.getValue(model.getSentinel());
    }

    private Expr put2(MapModel model, Expr key, Expr value, boolean shouldBeAbsent) {
        // copy inside stack
        model = copyModel(model);

        boolean shouldCheckForExisting = true;

        Expr previousValue = model.getSentinel();
        BoolExpr isAbsent = ctx.mkBool(true);

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        if (shouldCheckForExisting) {
            BoolExpr body = ctx.mkAnd(
                    ctx.mkLe(ctx.mkInt(0), i),
                    ctx.mkLt(i, model.getSize()),
                    ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                    ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key)
            );

            BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                    null, null, null, null);

            BoolExpr constraint = ctx.mkImplies(exists, body);
            Z3Translator.makeSolver().add(constraint);

            previousValue = ctx.mkITE(exists,
                    ctx.mkSelect(model.getArray(), i),
                    previousValue);
            isAbsent = ctx.mkNot(exists);
        }

        // check for existence (same as getEntry method)

        BoolExpr absenceSatisfiability = (BoolExpr) ctx.mkITE(ctx.mkBool(shouldBeAbsent),
                isAbsent, ctx.mkBool(true));

        // replace element
        ArrayExpr array = model.getArray();

        IntExpr index = (IntExpr) ctx.mkITE(isAbsent, model.getSize(), i);
        array = (ArrayExpr) ctx.mkITE(absenceSatisfiability,
                ctx.mkStore(array, index, model.mkDecl(key, value, ctx.mkBool(false), ctx.mkInt(stack.getIndex()))),
                array);

        ArithExpr newSize = (ArithExpr) ctx.mkITE(isAbsent,
                increment(ctx, model.getSize()),
                model.getSize());

        model.setArray(array);
        model.setSize(newSize);

        return model.getValue(previousValue);
    }

    private ArithExpr sizeReduced(MapModel model) {
        ArrayExpr array = model.getArray();
        ArithExpr size = model.getSize();
//        IntExpr stackSize = ctx.mkInt(stack.getIndex());

        IntExpr count = (IntExpr) ctx.mkFreshConst("count", ctx.mkIntSort());
        IntExpr index = (IntExpr) ctx.mkFreshConst("index", ctx.mkIntSort());

        // Create a function to represent the count at each index
        FuncDecl<IntSort> countFunc = ctx.mkFuncDecl("countFunc", ctx.getIntSort(), ctx.getIntSort());

        // Base case: count is 0 at index -1
        BoolExpr base = ctx.mkEq(ctx.mkApp(countFunc, ctx.mkInt(-1)), ctx.mkInt(0));

        // Recursive case: count at current index depends on previous index
        BoolExpr recursive = ctx.mkForall(new Expr[]{index},
                ctx.mkImplies(
                        ctx.mkAnd(
                                ctx.mkGe(index, ctx.mkInt(0)),
                                ctx.mkLt(index, size)
//                                ctx.mkGe(stackSize, model.getStackIndex(ctx.mkSelect(model.getArray(), index)))
                        ),
                        ctx.mkEq(
                                ctx.mkApp(countFunc, index),
                                ctx.mkITE(
                                        model.isEmpty(ctx.mkSelect(array, index)),
                                        ctx.mkApp(countFunc, ctx.mkSub(index, ctx.mkInt(1))),
                                        ctx.mkAdd(ctx.mkApp(countFunc, ctx.mkSub(index, ctx.mkInt(1))), ctx.mkInt(1))
                                )
                        )
                ),
                1, null, null, null, null
        );

        // The final count is the count at the last index (size - 1)
        BoolExpr finalCount = ctx.mkEq(count, ctx.mkApp(countFunc, ctx.mkSub(size, ctx.mkInt(1))));

        // Combine all constraints
        BoolExpr constraints = ctx.mkAnd(base, recursive, finalCount);
        Z3Translator.makeSolver().add(constraints);

        return count;
    }

    private void popEntriesFromStack(MapModel model, IntExpr stackLowerBound) {
        ArrayExpr updatedArray = arrayConstructor(ihc(model.getArray()));

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());

        BoolExpr condition = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkLt(stackLowerBound, model.getStackIndex(ctx.mkSelect(model.getArray(), i)))
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
    }

    private MapModel copyModel(MapModel model) {
        MapModel newModel = new MapModel(model);
        stack.add(newModel.getHashCode(), newModel);
        return newModel;

        // TODO: only copy model iff stack index has been passed
//        constructor(model.getHashCode(), model.getSize());
//        MapModel newModel = stack.get(model.getHashCode()).orElseThrow();
//        ArrayExpr array = model.getArray();
//        ArrayExpr newArray = newModel.getArray();
//        Z3Translator.makeSolver().add(ctx.mkEq(array, newArray));
////        ArrayExpr arrayCopy = ctx.mkStore(
////                model.getArray(),
////                ctx.mkInt(0),
////                ctx.mkSelect(array, ctx.mkInt(0)));
//        newModel.setArray(newArray);
//        newModel.setSize(model.getSize());
//        return newModel ;
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
