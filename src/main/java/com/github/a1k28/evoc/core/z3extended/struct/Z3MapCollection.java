package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

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
        return constructor(var1, ctx.mkInt(0));
    }

    public void asd(Expr var1) {
        MapModel model = stack.get(ihc(var1)).orElseThrow();
        System.out.println("ASD");
    }

    private Expr constructor(Expr var1, ArithExpr size) {
        int hashCode = ihc(var1);
//        TupleSort sort = mapSort(ctx, SortType.OBJECT.value(ctx), SortType.OBJECT.value(ctx));
        TupleSort sort = mkMapSort(ctx, ctx.mkStringSort(), ctx.mkStringSort());
        ArraySort arraySort = ctx.mkArraySort(ctx.mkIntSort(), sort);
        ArrayExpr array = (ArrayExpr) ctx.mkFreshConst("Map"+hashCode, arraySort);

        MapModel mapModel = new MapModel(
                array, size, sort, mkMapSentinel(ctx, ctx.mkStringSort(), ctx.mkStringSort()));
        stack.add(hashCode, mapModel);

        return var1;
    }

//    private Expr constructor(int hashCode,
//                            Integer capacity,
//                            Sort elementType,
//                            Expr[] arguments) {
//        List<Expr> l;
//        if (capacity != null) l = new ArrayList(capacity);
//        else if (arguments != null) l = new ArrayList(Arrays.stream(arguments).toList());
//        else l = new ArrayList();
//
////        ArrayExpr arrayExpr = ctx.mkArrayConst("ArrayList"+hashCode, ctx.mkIntSort(), elementType);
////        for (int i = 0; i < l.size(); i++)
////            ctx.mkStore(arrayExpr, ctx.mkInt(i), l.get(i));
//        Expr arrayExpr = ctx.mkConst("ArrayList"+hashCode, SortType.ARRAY.value(ctx));
//
//        stack.add(hashCode, new ListModel(arrayExpr, elementType, l));
//        return arrayExpr;
//    }

    public Expr get(Expr var1, Expr key) {
        MapModel model = getModel(var1);
        return model.getValue(getEntry(var1, key, false));
    }

    public Expr put(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, ctx.mkBool(false));
    }

    public Expr putIfAbsent(Expr var1, Expr key, Expr value) {
        return put(var1, key, value, ctx.mkBool(true));
    }

    public Expr size(Expr var1) {
        MapModel model = getModel(var1);
        ArithExpr knownSize = sizeReduced(model);
        return knownSize;
//        ArithExpr unknownSize = sizeReduced(model, true);
//        return ctx.mkAdd(knownSize, unknownSize);
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
                ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key)
        );
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

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

//
//    public BoolExpr add(Expr var1, Expr element) {
//        int hashCode = initList(var1);
//        int size = size(var1);
//        ListModel listModel = stack.get(hashCode).orElseThrow();
////        Sort sort = getSort(ctx, element);
////        if (!listModel.getSort().equals(sort))
//        listModel.getArguments().add(element);
////        ctx.mkStore(listModel.getExpr(), ctx.mkInt(listModel.getArguments().size()), element);
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr add(Expr var1, int index, Expr var2) {
//        int hashCode = initList(var1);
//        ListModel listModel = stack.get(hashCode).orElseThrow();
//        int size = listModel.getArguments().size();
//        if (index < 0 || index > size)
//            return ctx.mkBool(false);
//
//        listModel.getArguments().add(index, var2);
////        ctx.mkStore(listModel.getExpr(), ctx.mkInt(index), var2);
//
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr addAll(Expr var1, Expr var2) {
//        initList(var1);
//        int size = size(var1);
//        List<Expr> elements = get(var2).getArguments();
//        for (Expr expr : elements) add(var1, expr);
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr addAll(Expr var1, int index, Expr var2) {
//        initList(var1);
//        int size = size(var1);
//        List<Expr> elements = get(var2).getArguments();
//        for (int idx = index, i = 0; idx < index+elements.size(); idx++, i++)
//            add(var1, idx, elements.get(i));
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr remove(Expr var1, Integer... indices) {
//        int hashCode = ihc(var1);
//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkBool(false);
//
//        Set<Integer> idx = new HashSet<>(Arrays.stream(indices).toList());
//        for (int i = listModel.get().getArguments().size()-1; i >= 0; i--)
//            if (idx.contains(i))
//                listModel.get().getArguments().remove(i);
//
//        int size = size(var1);
////        constructor(hashCode,
////                null,
////                listModel.getSort(),
////                listModel.getArguments().toArray(new Expr[0]));
//
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr remove(Expr var1, Expr element) {
//        int hashCode = ihc(var1);
//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkBool(false);
//        int index = listModel.get().getArguments().indexOf(element);
//        return remove(var1, index);
//    }
//
//    public BoolExpr removeAll(Expr var1, Expr var2) {
//        int hashCode = ihc(var1);
//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkBool(false);
//        List<Expr> elements = get(var2).getArguments();
//        Integer[] indices = new Integer[elements.size()];
//        for (int i = 0; i < elements.size(); i++)
//            indices[i] = listModel.get().getArguments().indexOf(elements.get(i));
//        int size = size(var1);
//        remove(var1, indices);
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public BoolExpr contains(Expr var1, Expr element) {
//        int hashCode = ihc(var1);
//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkBool(false);
//        List<Expr> set = listModel.get().getArguments();
//        Expr[] expressions = new Expr[set.size()];
//        int i = 0;
//        for (Expr value : set) {
//            expressions[i] = ctx.mkEq(value, element);
//            i++;
//        }
//        return ctx.mkOr(expressions);
//    }
//
//    public BoolExpr containsAll(Expr var1, Expr var2) {
//        int hashCode = ihc(var1);
//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkBool(false);
//        List<Expr> set = listModel.get().getArguments();
//        List<Expr> elements = get(var2).getArguments();
//        Expr[] expressions = new Expr[elements.size()];
//        int i = 0;
//        for (Expr element : elements) {
//            Expr[] exps = new Expr[set.size()];
//            int j = 0;
//            for (Expr se : set) {
//                exps[j] = ctx.mkEq(se, element);
//                j++;
//            }
//            expressions[i] = ctx.mkOr(exps);
//            i++;
//        }
//        return ctx.mkAnd(expressions);
//    }
//
//    public BoolExpr retainAll(Expr var1, Expr var2) {
//        int size = size(var1);
//        List<Expr> elements = get(var2).getArguments();
//        for (Expr expr : get(var1).getArguments())
//            if (!elements.contains(expr))
//                remove(var1, expr);
//        int newSize = size(var1);
//        return ctx.mkBool(newSize != size);
//    }
//
//    public Expr clear(Expr var1) {
//        ListModel listModel = get(var1);
//        listModel.getArguments().clear();
//        return listModel.getExpr();
////        return constructor(ihc(var1),
////                null,
////                listModel.getSort(),
////                null);
//    }
//
//    // GYUUUUU
//    public BoolExpr equals(Expr var1, Expr var2) {
//        MapModel map1 = get(var1);
//        MapModel map2 = get(var2);
//        if (map1.getMap().size() != map2.getMap().size()) return ctx.mkBool(false);
//
//        Expr[] result = new Expr[map1.getMap().size()];
//        for (Map.Entry<Expr, Expr> entry : map1.getMap().entrySet()) {
//            Expr val2 = map2.getMap().get(entry.getKey()).toString();
//
//            Expr valEq = "null".equals(entry.getValue().toString()) ?
//                    "null".equals(map2.getMap().get(entry.getKey()).toString()) :
//            Expr valEq = ctx.mkEq(entry.getValue(), map2.getMap().get(entry.getKey()));
//            result[i] = ctx.mkAnd()
//        }
//        for (int i = 0; i < elements.size(); i++)
//            result[i] = ctx.mkEq(elements.get(i), exprs.get(i));
//        return ctx.mkAnd(result);
//    }
//
//    public Expr get(Expr var1, Expr key) {
//        MapModel mapModel = get(var1);
//        if (!mapModel.getMap().containsKey(key)) return ctx.mkConst("null", mapModel.getValueSort());
//        return mapModel.getMap().get(key);
//    }
//
//    public Expr getOrDefault(Expr var1, Expr key, Expr defaultValue) {
//        MapModel mapModel = get(var1);
//        if (!mapModel.getMap().containsKey(key)) return defaultValue;
//        return mapModel.getMap().get(key);
//    }
//
//    public Expr hashCode(Expr var1) {
//        return ctx.mkInt(get(var1).getMap().hashCode());
//    }
//
//    private int size(Expr var1) {
//        return get(var1).getMap().size();
//    }

    private Expr getEntry(Expr var1, Expr expr, boolean valueComparison) {
        MapModel model = getModel(var1);

        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(getKeyOrValue(model, ctx.mkSelect(model.getArray(), i), valueComparison), expr)
        );

        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        Expr e = ctx.mkITE(exists,
                ctx.mkSelect(model.getArray(), i),
                model.getSentinel());
        return e;
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
//        ArithExpr size = model.getKnownSize();
//        ArrayExpr array = model.getKnownList();
//        array = ctx.mkStore(array, size, model.mkDecl(key, value, ctx.mkBool(false)));
//        size = increment(ctx, size);
//        model.setKnownList(array);
//        model.setKnownSize(size);
//        if (true) return null;
        // check for existence
        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.getIntSort());
        BoolExpr body = ctx.mkAnd(
                ctx.mkLe(ctx.mkInt(0), i),
                ctx.mkLt(i, model.getSize()),
                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getArray(), i))),
                ctx.mkEq(model.getKey(ctx.mkSelect(model.getArray(), i)), key)
        );
        BoolExpr exists = ctx.mkExists(new Expr[]{i}, body, 1,
                null, null, null, null);

        // Create assertions
//        BoolExpr assertions = ctx.mkAnd(
//                ctx.mkEq(i, ctx.mkInt(0)),  // Initialize index to 0
//                existsKnown
//        );

        // get previous value
        Expr previousValue = ctx.mkITE(exists,
                model.getValue(ctx.mkSelect(model.getArray(), i)),
                model.getValue(model.getSentinel()));

        BoolExpr isAbsent = ctx.mkAnd(ctx.mkNot(exists));
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

        return previousValue;
    }

//    private Expr put(MapModel model, Expr key, Expr value, BoolExpr shouldBeAbsent) {
//        // check for existence
//        IntExpr i = (IntExpr) ctx.mkConst("i", ctx.getIntSort());
//        BoolExpr knownBody = ctx.mkAnd(
//                ctx.mkLe(ctx.mkInt(0), i),
//                ctx.mkLt(i, model.getKnownSize()),
//                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getKnownList(), i))),
//                ctx.mkEq(model.getKey(ctx.mkSelect(model.getKnownList(), i)), key)
//        );
//        BoolExpr existsKnown = ctx.mkExists(new Expr[]{i}, knownBody, 1,
//                null, null, null, null);
//
//        IntExpr j = (IntExpr) ctx.mkConst("j", ctx.getIntSort());
//        BoolExpr unknownBody = ctx.mkAnd(
//                ctx.mkNot(existsKnown),
//                ctx.mkLe(ctx.mkInt(0), j),
//                ctx.mkLt(j, model.getUnknownSize()),
//                ctx.mkNot(model.isEmpty(ctx.mkSelect(model.getUnknownList(), j))),
//                ctx.mkEq(model.getKey(ctx.mkSelect(model.getUnknownList(), j)), key)
//        );
//        BoolExpr existsUnknown = ctx.mkExists(new Expr[]{j}, unknownBody, 1,
//                null, null, null, null);
//
//        // get previous value
//        Expr unknownValue = ctx.mkITE(existsUnknown,
//                model.getValue(ctx.mkSelect(model.getUnknownList(), j)),
//                model.getValue(model.getSentinel()));
//
//        Expr previousValue = ctx.mkITE(existsKnown,
//                model.getValue(ctx.mkSelect(model.getKnownList(), i)),
//                unknownValue);
//
//        BoolExpr isAbsent = ctx.mkAnd(ctx.mkNot(existsKnown), ctx.mkNot(existsUnknown));
//        BoolExpr absenceSatisfiability = (BoolExpr) ctx.mkITE(shouldBeAbsent,
//                isAbsent, ctx.mkBool(true));
//
//        // replace element
//        ArrayExpr knownArray = model.getKnownList();
//        ArrayExpr unknownArray = model.getUnknownList();
//
//        knownArray = (ArrayExpr) ctx.mkITE(ctx.mkAnd(absenceSatisfiability, ctx.mkNot(existsUnknown)),
//                ctx.mkStore(knownArray, ctx.mkAdd(i, ctx.mkInt(1)), model.mkDecl(key, value, ctx.mkBool(false))),
//                knownArray);
//
//        unknownArray = (ArrayExpr) ctx.mkITE(ctx.mkAnd(absenceSatisfiability, existsUnknown),
//                ctx.mkStore(unknownArray, ctx.mkAdd(j, ctx.mkInt(1)), model.mkDecl(key, value, ctx.mkBool(false))),
//                unknownArray);
//
//        ArithExpr newKnownSize = (ArithExpr) ctx.mkITE(isAbsent,
//                ctx.mkAdd(model.getKnownSize(), ctx.mkInt(1)),
//                model.getKnownSize());
//
//        model.setKnownList(knownArray);
//        model.setUnknownList(unknownArray);
//        model.setKnownSize(newKnownSize);
//
//        return previousValue;
//    }

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
        int hashCode = ihc(var1);
        if (stack.get(hashCode).isEmpty()) {
            ArithExpr size = (ArithExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            constructor(var1, size);
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
