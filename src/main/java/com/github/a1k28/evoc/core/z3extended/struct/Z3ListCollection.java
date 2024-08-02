package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.ListModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.*;

import static com.github.a1k28.evoc.core.z3extended.Z3Helper.getSort;

public class Z3ListCollection implements IStack {

    private final Context ctx;
    private final Z3Stack<Integer, ListModel> stack;
    private final Map<Sort, Expr> sentinels;

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public Z3ListCollection(Context context) {
        this.ctx = context;
        this.stack = new Z3Stack<>();
        this.sentinels = new HashMap<>();
    }

    public Expr constructor(Expr var1) {
        return constructor(ihc(var1),
                null,
                SortType.OBJECT.value(ctx),
                null);
    }

    public Expr constructor(Expr var1,
                            Integer capacity) {
        return constructor(ihc(var1),
                capacity,
                SortType.ARRAY.value(ctx),
                null);
    }

    public Expr constructor(Expr var1,
                            Expr var2) {
        Expr[] arguments = getArguments(get(var2));
        Sort sort = getSort(ctx, arguments);
        return constructor(
                ihc(var1),
                null,
                sort,
                arguments);
    }

    public Expr constructor(Sort elementType, Expr[] arguments) {
        return constructor(UUID.randomUUID().toString().hashCode(),
                null, elementType, arguments);
    }

    private Expr constructor(int hashCode,
                            Integer capacity,
                            Sort elementType,
                            Expr[] arguments) {
        ArrayExpr arrayExpr = ctx.mkArrayConst("Array"+hashCode, ctx.mkIntSort(), elementType);
        stack.add(hashCode, new ListModel(arrayExpr, elementType, mkSentinel(elementType), capacity));
        if (arguments != null)
            for (Expr argument : arguments)
                add(arrayExpr, argument);
        return arrayExpr;
    }

    public Expr size(Expr var1) { // X
        ListModel listModel = get(var1);
        return size(listModel);
    }

    public BoolExpr isEmpty(Expr var1) { // X
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    // TODO: capacity constraints?
    public BoolExpr add(Expr var1, Expr element) { // X
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, element);

        ArrayExpr array = listModel.getExpr();
        BoolExpr capacitySatisfied = ctx.mkGt(ctx.mkInt(listModel.getCapacity()), size(var1));
        Expr elem = ctx.mkITE(capacitySatisfied, element, listModel.getSentinel());
        array = ctx.mkStore(array, ctx.mkInt(listModel.getSize()), elem);
        listModel.setExpr(array);

        listModel.incrementSize();
        return capacitySatisfied;
    }

    public BoolExpr add(Expr var1, ArithExpr index, Expr var2) { // X
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, var2);

        Expr idx = translateIndex(listModel, index);
        ArrayExpr array = listModel.getExpr();
        BoolExpr capacitySatisfied = ctx.mkGt(ctx.mkInt(listModel.getCapacity()), size(var1));
        for (int i = listModel.getSize(); i > 0; i--) {
            Expr ie = ctx.mkInt(i);
            Expr currentValue = ctx.mkSelect(array, ie);
            Expr previousValue = ctx.mkSelect(array, ctx.mkInt(i-1));
            BoolExpr shouldShift = ctx.mkGt(ie, idx);
            Expr shiftExpr = ctx.mkITE(shouldShift, previousValue, currentValue);
            Expr elem = ctx.mkITE(capacitySatisfied, shiftExpr, listModel.getSentinel());
            array = ctx.mkStore(array, ie, elem);
        }

        Expr elem = ctx.mkITE(capacitySatisfied, var2, listModel.getSentinel());
        array = ctx.mkStore(array, idx, elem);
        listModel.setExpr(array);
        listModel.incrementSize();
        return ctx.mkBool(true);
    }

    public BoolExpr addAll(Expr var1, Expr var2) { // X
        Optional<ListModel> listModel = getOptional(var2);
        if (listModel.isEmpty() || listModel.get().getSize() == 0)
            return ctx.mkBool(false);

        ArrayExpr array = listModel.get().getExpr();
        Expr originalSize = size(listModel.get());

        for (int i = 0; i < listModel.get().getSize(); i++)
            add(var1, ctx.mkSelect(array, ctx.mkInt(i)));

        Expr newSize = size(listModel.get());
        return ctx.mkGt(newSize, originalSize);
    }

    public BoolExpr addAll(Expr var1, ArithExpr index, Expr var2) { // X
        ListModel listModel = get(var2);
        ArrayExpr array = listModel.getExpr();
        Expr originalSize = size(listModel);

        for (int i = 0; i < listModel.getSize(); i++) {
            ArithExpr idx = translateIndex(get(var1), index);
            add(var1, idx, ctx.mkSelect(array, ctx.mkInt(i)));
            index = ctx.mkAdd(index, ctx.mkInt(1));
        }

        Expr newSize = size(listModel);
        return ctx.mkGt(newSize, originalSize);
    }

    public BoolExpr remove(Expr var1, IntExpr index) { // X
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Expr idx = translateIndex(listModel.get(), index);

        Expr sentinel = listModel.get().getSentinel();
        ArrayExpr array = listModel.get().getExpr();
        BoolExpr found = ctx.mkBool(false);
        for (int i = 0; i < listModel.get().getSize(); i++) {
            Expr ie = ctx.mkInt(i);
            Expr currentValue = ctx.mkSelect(array, ie);
            BoolExpr isMatch = ctx.mkEq(idx, ie);

            array = ctx.mkStore(array, ie,
                    ctx.mkITE(isMatch, sentinel, currentValue));

            found = ctx.mkOr(found, isMatch);
        }

        listModel.get().setExpr(array);
        return found;
    }

    // remove the first occurrence of the element
    public BoolExpr remove(Expr var1, Expr element) { // X
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        return remove(listModel.get(), element, false);
    }

    public BoolExpr removeAll(Expr var1, Expr var2) { // X
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        BoolExpr found = ctx.mkBool(false);
        ListModel targetModel = get(var2);
        ArrayExpr array = targetModel.getExpr();
        for (int i = 0; i < targetModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            found = ctx.mkOr(found, remove(listModel.get(), currentValue, true));
        }
        return found;
    }

    public BoolExpr contains(Expr var1, Expr element) { // X
        return contains(var1, null, element);
    }

    public BoolExpr containsAll(Expr var1, Expr var2) { // X
        Optional<ListModel> listModel = getOptional(var2);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        Expr[] expressions = getArguments(listModel.get());
        return contains(var1, listModel.get().getSentinel(), expressions);
    }

    public BoolExpr retainAll(Expr var1, Expr var2) { // X
        ListModel lm1 = get(var1);
        ListModel lm2 = get(var2);

        Expr originalSize = size(lm1);
        Expr set = ctx.mkEmptySet(lm2.getSort());
        for (int i = 0; i < lm2.getSize(); i++)
            set = ctx.mkSetAdd(set, ctx.mkSelect(lm2.getExpr(), ctx.mkInt(i)));

        ArrayExpr array = lm1.getExpr();
        for (int i = 0; i < lm1.getSize(); i++) {
            Expr currElement = ctx.mkSelect(array, ctx.mkInt(i));
            Expr expr = ctx.mkITE(ctx.mkSetMembership(currElement, set), currElement, lm1.getSentinel());
            array = ctx.mkStore(array, ctx.mkInt(i), expr);
        }
        lm1.setExpr(array);
        Expr newSize = size(lm1);

        return ctx.mkNot(ctx.mkEq(originalSize, newSize));
    }

    public Expr clear(Expr var1) { // X
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return null;
        ArrayExpr array = listModel.get().getExpr();
        for (int i = 0; i < listModel.get().getSize(); i++)
            array = ctx.mkStore(array, ctx.mkInt(i), listModel.get().getSentinel());
        listModel.get().setExpr(array);
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) { // X
        ListModel lm1 = get(var1);
        ListModel lm2 = get(var2);

        if (lm1.getSize() != lm2.getSize())
            return ctx.mkEq(size(lm1), size(lm2));

        BoolExpr[] exprs = new BoolExpr[lm1.getSize()];
        for (int i = 0; i < lm1.getSize(); i++) {
            Expr v1 = ctx.mkSelect(lm1.getExpr(), ctx.mkInt(i));
            Expr v2 = ctx.mkSelect(lm2.getExpr(), ctx.mkInt(i));
            exprs[i] = ctx.mkEq(v1, v2);
        }
        return ctx.mkAnd(exprs);
    }

    public Expr get(Expr var1, IntExpr index) { // X
        ListModel listModel = get(var1);
        Expr idx = translateIndex(listModel, index);
        return ctx.mkSelect(listModel.getExpr(), idx);
    }

    public Expr set(Expr var1, IntExpr index, Expr element) { // X
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, element);
        Expr idx = translateIndex(listModel, index);
        ArrayExpr array = listModel.getExpr();
        Expr prevElement = ctx.mkSelect(array, idx);
        array = ctx.mkStore(array, idx, element);
        listModel.setExpr(array);
        return prevElement;
    }

    // TODO: what to do here?
    public Expr hashCode(Expr var1) {
        return ctx.mkInt(Arrays.stream(getArguments(get(var1))).toList().hashCode());
    }

    public Expr subList(Expr var1, IntExpr fromIndex, IntExpr toIndex) { // X
        ListModel listModel = get(var1);
        Expr fromIdx = translateIndex(listModel, fromIndex);
        Expr toIdx = translateIndex(listModel, toIndex);
        Expr[] arguments = getArguments(listModel);
        Expr[] expressions = new Expr[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Expr element = ctx.mkSelect(listModel.getExpr(), ctx.mkInt(i));
            BoolExpr isMatch = ctx.mkAnd(
                    ctx.mkGe(ctx.mkInt(i), fromIdx),
                    ctx.mkLt(ctx.mkInt(i), toIdx));
            expressions[i] = ctx.mkITE(isMatch, element, listModel.getSentinel());
        }
        return constructor(getSort(ctx, expressions), expressions);
    }

    public Expr indexOf(Expr var1, Expr element) { // X
        Expr sentinel = ctx.mkInt(-1);
        ListModel listModel = get(var1);
        Expr[] expressions = getArguments(listModel);
        Expr foundElement = sentinel;
        for (int i = expressions.length-1; i >= 0; i--)
            foundElement = ctx.mkITE(ctx.mkEq(expressions[i], element),
                    translateIndex(listModel, ctx.mkInt(i)), sentinel);
        return foundElement;
    }

    public Expr lastIndexOf(Expr var1, Expr element) { // X
        Expr sentinel = ctx.mkInt(-1);
        ListModel listModel = get(var1);
        Expr[] expressions = getArguments(listModel);
        Expr foundElement = sentinel;
        for (int i = 0; i < expressions.length; i++)
            foundElement = ctx.mkITE(ctx.mkEq(expressions[i], element),
                    translateIndex(listModel, ctx.mkInt(i)), sentinel);
        return foundElement;
    }

    private ListModel get(Expr var1) {
        int hashCode = initList(var1);
        return stack.get(hashCode).orElseThrow();
    }

    private Optional<ListModel> getOptional(Expr var1) {
        int hashCode = ihc(var1);
        return stack.get(hashCode);
    }

    private int initList(Expr var1) {
        int hashCode = ihc(var1);
        if (stack.get(hashCode).isEmpty())
            constructor(var1);
        return hashCode;
    }

    private Expr mkSentinel(Sort sort) {
        if (!sentinels.containsKey(sort))
            sentinels.put(sort, ctx.mkConst("null", sort));
        return sentinels.get(sort);
    }

    private Expr size(ListModel listModel) {
        ArithExpr newSize = ctx.mkInt(0);
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr condition = ctx.mkEq(ctx.mkSelect(listModel.getExpr(), ctx.mkInt(i)), listModel.getSentinel());
            Expr thenExpr = ctx.mkInt(0);
            Expr elseExpr = ctx.mkInt(1);
            newSize = ctx.mkAdd(newSize, ctx.mkITE(condition, thenExpr, elseExpr));
        }
        return newSize;
    }

    private BoolExpr remove(ListModel listModel, Expr element, boolean removeAllOccurrences) {
        ArrayExpr array = listModel.getExpr();
        BoolExpr found = ctx.mkBool(false);
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isMatch = ctx.mkEq(currentValue, element);
            if (!removeAllOccurrences) isMatch = ctx.mkAnd(isMatch, ctx.mkNot(found));

            array = ctx.mkStore(array, ctx.mkInt(i),
                    ctx.mkITE(isMatch, listModel.getSentinel(), currentValue));

            found = ctx.mkOr(found, isMatch);
        }

        listModel.setExpr(array);
        return found;
    }

    private BoolExpr contains(Expr var1, Expr elementSentinel, Expr... elements) {
        if (elements == null) return ctx.mkBool(false);

        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Expr sentinel = listModel.get().getSentinel();
        ArrayExpr array = listModel.get().getExpr();
        Expr count = ctx.mkInt(0);
        for (int i = 0; i < listModel.get().getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            Expr isPresent = ctx.mkITE(ctx.mkEq(currentValue, sentinel),
                    ctx.mkBool(false), ctx.mkBool(true));
            BoolExpr isMatch = ctx.mkBool(false);
            for (Expr element : elements) {
                BoolExpr isMatch2 = ctx.mkEq(currentValue, element);
                if (elementSentinel != null) {
                    Expr isPresent2 = ctx.mkITE(ctx.mkEq(element, elementSentinel),
                            ctx.mkBool(false), ctx.mkBool(true));
                    isMatch2 = ctx.mkAnd(isPresent2, isMatch2);
                }
                isMatch = ctx.mkOr(isMatch, isMatch2);
            }
            isMatch = ctx.mkAnd(isPresent, isMatch);
            count = ctx.mkAdd(count, ctx.mkITE(isMatch, ctx.mkInt(1), ctx.mkInt(0)));
        }
        return ctx.mkEq(count, ctx.mkInt(elements.length));
    }

    private Expr[] getArguments(ListModel listModel) {
        Expr[] expressions = new Expr[listModel.getSize()];
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isRemoved = ctx.mkEq(currentValue, listModel.getSentinel());
//            expressions[i] = ctx.mkIff(currentValue, ctx.mkNot(isRemoved));
            expressions[i] = currentValue;
        }
        return expressions;
    }

    // TODO: implement hierarchy checks
    private void validateAndReplaceModelWithNewSort(Expr var1, ListModel listModel, Expr element) {
        Sort elementType = getSort(ctx, element);
        if (!listModel.getSort().equals(elementType)) {
            ArrayExpr arrayExpr = ctx.mkArrayConst("Array"+ihc(var1),
                    ctx.mkIntSort(), elementType);
            for (int i = 0; i < listModel.getSize(); i++)
                arrayExpr = ctx.mkStore(arrayExpr, ctx.mkInt(i),
                        ctx.mkSelect(listModel.getExpr(), ctx.mkInt(i)));
            listModel.setExpr(arrayExpr);
            listModel.setSort(elementType);
            listModel.setSentinel(mkSentinel(elementType));
        }
    }

    // translate index from int to expr in order to consider removed elements
    private ArithExpr translateIndex(ListModel listModel, ArithExpr index) {
        Expr sentinel = listModel.getSentinel();
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currElement = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr shouldContinue = ctx.mkGe(index, ctx.mkInt(i));
            BoolExpr wasRemoved = ctx.mkEq(currElement, sentinel);
            ArithExpr idxExpr = (ArithExpr) ctx.mkITE(wasRemoved, ctx.mkAdd(index, ctx.mkInt(1)), index);
            index = (ArithExpr) ctx.mkITE(shouldContinue, idxExpr, index);
        }
        return index;
    }

    private static int ihc(Object o) {
        if (o instanceof Expr) {
            if (o.toString().contains("Array")) {
                try {
                    return Integer.parseInt(o.toString().replace("Array", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return System.identityHashCode(o);
    }
}
