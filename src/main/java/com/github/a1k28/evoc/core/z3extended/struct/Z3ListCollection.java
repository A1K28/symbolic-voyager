package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.ListModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.*;

import static com.github.a1k28.evoc.core.z3extended.Z3Helper.*;

public class Z3ListCollection implements IStack {

    private final Context ctx;
    private final Z3Stack<Integer, ListModel> stack;

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
        Expr[] arguments = getArgumentValues(get(var2));
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
        TupleSort sort = mkListSort(ctx, elementType);
        ArrayExpr arrayExpr = ctx.mkArrayConst("Array"+hashCode, ctx.mkIntSort(), sort);
        stack.add(hashCode, new ListModel(arrayExpr, sort, mkListSentinel(ctx, elementType), capacity));
        if (arguments != null)
            for (Expr argument : arguments)
                add(arrayExpr, argument);
        return arrayExpr;
    }

    public Expr size(Expr var1) {
        ListModel listModel = get(var1);
        return size(listModel);
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(size(var1), ctx.mkInt(0));
    }

    public BoolExpr add(Expr var1, Expr element) {
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, element);

        ArrayExpr array = listModel.getExpr();
        BoolExpr capacitySatisfied = ctx.mkGt(ctx.mkInt(listModel.getCapacity()), size(var1));
        BoolExpr isEmpty = ctx.mkNot(capacitySatisfied);
        Expr optional = listModel.mkDecl(element, isEmpty);
        array = ctx.mkStore(array, ctx.mkInt(listModel.getSize()), optional);
        listModel.setExpr(array);

        listModel.incrementSize();
        return capacitySatisfied;
    }

    public BoolExpr add(Expr var1, ArithExpr index, Expr var2) {
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, var2);

        // verify index
        Expr size = size(var1);
        Expr idx = translateIndex(listModel, index);
        BoolExpr isInvalidIdx = ctx.mkGt(idx, size);
        idx = ctx.mkITE(isInvalidIdx, size, idx);

        // init vars
        ArrayExpr array = listModel.getExpr();
        BoolExpr capacitySatisfied = ctx.mkGt(ctx.mkInt(listModel.getCapacity()), size);

        // store empty element at the end
        // the element will either be replaced, or stay intact
        array = ctx.mkStore(array, size, listModel.getSentinel());

        // shift elements to the right by 1
        for (int i = listModel.getSize(); i > 0; i--) {
            Expr ie = ctx.mkInt(i);
            Expr currentValue = ctx.mkSelect(array, ie);
            Expr previousValue = ctx.mkSelect(array, ctx.mkInt(i-1));
            BoolExpr shouldShift = ctx.mkAnd(capacitySatisfied, ctx.mkGt(ie, idx));
            Expr shiftExpr = ctx.mkITE(shouldShift, previousValue, currentValue);
            array = ctx.mkStore(array, ie, shiftExpr);
        }

        // replace the element at index
        var2 = listModel.mkDecl(var2, ctx.mkBool(false));
        Expr elem = ctx.mkITE(capacitySatisfied, var2, ctx.mkSelect(array, idx));
        array = ctx.mkStore(array, idx, elem);

        // update state & return
        listModel.setExpr(array);
        listModel.incrementSize();
        return capacitySatisfied;
    }

    public BoolExpr addAll(Expr var1, Expr var2) {
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

    public BoolExpr addAll(Expr var1, ArithExpr index, Expr var2) {
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

    public Expr remove(Expr var1, IntExpr index) {
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Expr idx = translateIndex(listModel.get(), index);
        Expr sentinel = listModel.get().getSentinel();
        ArrayExpr array = listModel.get().getExpr();
        Expr val = sentinel;
        for (int i = 0; i < listModel.get().getSize(); i++) {
            Expr ie = ctx.mkInt(i);
            Expr currentValue = ctx.mkSelect(array, ie);
            BoolExpr isMatch = ctx.mkEq(idx, ie);

            val = ctx.mkITE(isMatch, currentValue, sentinel);

            array = ctx.mkStore(array, ie,
                    ctx.mkITE(isMatch, sentinel, currentValue));
        }

        listModel.get().setExpr(array);
        return listModel.get().getValue(val);
    }

    // remove the first occurrence of the element
    public BoolExpr remove(Expr var1, Expr element) {
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        return remove(listModel.get(), element);
    }

    public BoolExpr removeAll(Expr var1, Expr var2) {
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Expr originalSize = size(listModel.get());
        ListModel targetModel = get(var2);
        ArrayExpr array = targetModel.getExpr();
        for (int i = 0; i < targetModel.getSize(); i++) {
            Expr currentValue = targetModel.getValue(ctx.mkSelect(array, ctx.mkInt(i)));
            removeAllOccurrences(listModel.get(), currentValue);
        }
        Expr newSize = size(listModel.get());
        return ctx.mkLt(newSize, originalSize);
    }

    public BoolExpr contains(Expr var1, Expr element) {
        return containsElements(var1, element);
    }

    public BoolExpr containsAll(Expr var1, Expr var2) {
        Optional<ListModel> listModel = getOptional(var2);
        if (listModel.isEmpty()) return ctx.mkBool(true); // TODO: check ?
        Expr[] expressions = getArgumentValues(listModel.get());
        return containsElements(var1, expressions);
    }

    public BoolExpr retainAll(Expr var1, Expr var2) {
        ListModel lm1 = get(var1);
        ListModel lm2 = get(var2);

        Expr originalSize = size(lm1);
        Expr set = ctx.mkEmptySet(lm2.getSort());
        for (int i = 0; i < lm2.getSize(); i++)
            set = ctx.mkSetAdd(set, lm2.getValue(ctx.mkSelect(lm2.getExpr(), ctx.mkInt(i))));

        ArrayExpr array = lm1.getExpr();
        for (int i = 0; i < lm1.getSize(); i++) {
            Expr currElement = lm1.getValue(ctx.mkSelect(array, ctx.mkInt(i)));
            Expr expr = ctx.mkITE(ctx.mkSetMembership(currElement, set), currElement, lm1.getSentinel());
            array = ctx.mkStore(array, ctx.mkInt(i), expr);
        }
        lm1.setExpr(array);
        Expr newSize = size(lm1);

        return ctx.mkNot(ctx.mkEq(originalSize, newSize));
    }

    public Expr clear(Expr var1) {
        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return null;
        ArrayExpr array = listModel.get().getExpr();
        for (int i = 0; i < listModel.get().getSize(); i++)
            array = ctx.mkStore(array, ctx.mkInt(i), listModel.get().getSentinel());
        listModel.get().setExpr(array);
        return null;
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        ListModel lm1 = get(var1);
        ListModel lm2 = get(var2);

        if (lm1.getSize() != lm2.getSize())
            return ctx.mkEq(size(lm1), size(lm2));

        BoolExpr[] exprs = new BoolExpr[lm1.getSize()];
        for (int i = 0; i < lm1.getSize(); i++) {
            Expr element1 = ctx.mkSelect(lm1.getExpr(), ctx.mkInt(i));
            Expr element2 = ctx.mkSelect(lm2.getExpr(), ctx.mkInt(i));

            BoolExpr isEmpty1 = lm1.isEmpty(element1);
            BoolExpr isEmpty2 = lm1.isEmpty(element2);

            Expr value1 = lm1.getValue(element1);
            Expr value2 = lm2.getValue(element2);

            BoolExpr isEmpty = ctx.mkAnd(isEmpty1, isEmpty2);
            BoolExpr isEqual = ctx.mkEq(value1, value2);
            exprs[i] = ctx.mkOr(isEmpty, isEqual);
        }
        return ctx.mkAnd(exprs);
    }

    public Expr get(Expr var1, IntExpr index) {
        ListModel listModel = get(var1);
        Expr idx = translateIndex(listModel, index);
        return listModel.getValue(ctx.mkSelect(listModel.getExpr(), idx));
    }

    public Expr set(Expr var1, IntExpr index, Expr element) {
        ListModel listModel = get(var1);
        validateAndReplaceModelWithNewSort(var1, listModel, element);
        Expr idx = translateIndex(listModel, index);
        ArrayExpr array = listModel.getExpr();
        Expr prevElement = listModel.getValue(ctx.mkSelect(array, idx));
        array = ctx.mkStore(array, idx, listModel.mkDecl(element, ctx.mkBool(false)));
        listModel.setExpr(array);
        return prevElement;
    }

    // TODO: what to do here?
    public Expr hashCode(Expr var1) {
        return ctx.mkInt(Arrays.stream(getArgumentValues(get(var1))).toList().hashCode());
    }

    public Expr subList(Expr var1, IntExpr fromIndex, IntExpr toIndex) {
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
        Expr[] values = new Expr[arguments.length];
        for (int i = 0; i < arguments.length; i++)
            values[i] = listModel.getValue(expressions[i]);
        return constructor(getSort(ctx, values), expressions);
    }

    public Expr indexOf(Expr var1, Expr element) {
        Expr sentinel = ctx.mkInt(-1);
        ListModel listModel = get(var1);
        Expr[] expressions = getArgumentValues(listModel);
        Expr foundElement = sentinel;
        for (int i = expressions.length-1; i >= 0; i--) {
            BoolExpr equals = ctx.mkEq(expressions[i], element);
            foundElement = ctx.mkITE(equals, translateIndexInverted(listModel, i), sentinel);
        }
        return foundElement;
    }

    public Expr lastIndexOf(Expr var1, Expr element) {
        Expr sentinel = ctx.mkInt(-1);
        ListModel listModel = get(var1);
        Expr[] expressions = getArgumentValues(listModel);
        Expr foundElement = sentinel;
        for (int i = 0; i < expressions.length; i++) {
            BoolExpr equals = ctx.mkEq(expressions[i], element);
            foundElement = ctx.mkITE(equals, translateIndexInverted(listModel, i), sentinel);
        }
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

    private Expr size(ListModel listModel) {
        ArithExpr newSize = ctx.mkInt(0);
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr value = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isEmpty = listModel.isEmpty(value);
            Expr thenExpr = ctx.mkInt(0);
            Expr elseExpr = ctx.mkInt(1);
            newSize = ctx.mkAdd(newSize, ctx.mkITE(isEmpty, thenExpr, elseExpr));
        }
        return newSize;
    }

    private BoolExpr remove(ListModel listModel, Expr element) {
        ArrayExpr array = listModel.getExpr();
        BoolExpr found = ctx.mkBool(false);
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isMatch = ctx.mkEq(listModel.getValue(currentValue), element);
            isMatch = ctx.mkAnd(isMatch, ctx.mkNot(found));
            array = ctx.mkStore(array, ctx.mkInt(i),
                    ctx.mkITE(isMatch, listModel.getSentinel(), currentValue));
            found = ctx.mkOr(found, isMatch);
        }
        listModel.setExpr(array);
        return found;
    }

    private void removeAllOccurrences(ListModel listModel, Expr element) {
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isMatch = ctx.mkEq(listModel.getValue(currentValue), element);
            array = ctx.mkStore(array, ctx.mkInt(i),
                    ctx.mkITE(isMatch, listModel.getSentinel(), currentValue));
        }
        listModel.setExpr(array);
    }

    private BoolExpr containsElements(Expr var1, Expr... elements) {
        if (elements == null) return ctx.mkBool(false);

        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        ArrayExpr array = listModel.get().getExpr();
        Expr count = ctx.mkInt(0);
        for (int i = 0; i < listModel.get().getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            Expr isPresent = ctx.mkNot(listModel.get().isEmpty(currentValue));
            BoolExpr isMatch = ctx.mkBool(false);
            for (Expr element : elements) {
                BoolExpr isMatch2 = ctx.mkEq(currentValue, element);
                Expr isPresent2 = ctx.mkNot(listModel.get().isEmpty(element));
                isMatch2 = ctx.mkAnd(isPresent2, isMatch2);
                isMatch = ctx.mkOr(isMatch, isMatch2);
            }
            isMatch = ctx.mkAnd(isPresent, isMatch);
            count = ctx.mkAdd(count, ctx.mkITE(isMatch, ctx.mkInt(1), ctx.mkInt(0)));
        }
        return ctx.mkEq(count, ctx.mkInt(elements.length));
    }

    private Expr[] getArgumentValues(ListModel listModel) {
        Expr[] expressions = new Expr[listModel.getSize()];
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            expressions[i] = listModel.getValue(currentValue);
        }
        return expressions;
    }

    private Expr[] getArguments(ListModel listModel) {
        Expr[] expressions = new Expr[listModel.getSize()];
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            expressions[i] = currentValue;
        }
        return expressions;
    }

    // TODO: implement hierarchy checks
    private void validateAndReplaceModelWithNewSort(Expr var1, ListModel listModel, Expr element) {
        Sort elementType = getSort(ctx, element);
        if (!listModel.getSort().equals(elementType)) {
            TupleSort sort = mkListSort(ctx, elementType);
            ArrayExpr arrayExpr = ctx.mkArrayConst("Array"+ihc(var1),
                    ctx.mkIntSort(), sort);
            for (int i = 0; i < listModel.getSize(); i++)
                arrayExpr = ctx.mkStore(arrayExpr, ctx.mkInt(i),
                        ctx.mkSelect(listModel.getExpr(), ctx.mkInt(i)));
            listModel.setExpr(arrayExpr);
            listModel.setSort(sort);
            listModel.setSentinel(mkListSentinel(ctx, elementType));
        }
    }

    // translate index to consider removed elements
    private ArithExpr translateIndex(ListModel listModel, ArithExpr index) {
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getSize(); i++) {
            Expr currElement = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr shouldContinue = ctx.mkGe(index, ctx.mkInt(i));
            BoolExpr isEmpty = listModel.isEmpty(currElement);
            ArithExpr idxExpr = (ArithExpr) ctx.mkITE(isEmpty, ctx.mkAdd(index, ctx.mkInt(1)), index);
            index = (ArithExpr) ctx.mkITE(shouldContinue, idxExpr, index);
        }
        return index;
    }

    private ArithExpr translateIndexInverted(ListModel listModel, int index) {
        ArrayExpr array = listModel.getExpr();
        ArithExpr idx = ctx.mkInt(index);
        for (int i = 0; i < index; i++) {
            Expr currElement = ctx.mkSelect(array, ctx.mkInt(i));
            BoolExpr isEmpty = listModel.isEmpty(currElement);
            idx = (ArithExpr) ctx.mkITE(isEmpty, ctx.mkSub(idx, ctx.mkInt(1)), idx);
        }
        return idx;
    }

    private static int ihc(Object o) {
        if (o instanceof Expr && o.toString().contains("Array")) {
            try {
                return Integer.parseInt(o.toString().replace("Array", ""));
            } catch (NumberFormatException ignored) {}
        }
        return System.identityHashCode(o);
    }
}
