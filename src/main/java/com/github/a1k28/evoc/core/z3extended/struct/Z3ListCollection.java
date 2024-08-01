package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.ListModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.*;

import static com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext.getSort;

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
                SortType.OBJECT.value(ctx),
                null);
    }

    public Expr constructor(Expr var1,
                            Expr var2) {
        initList(var2);
        List<Expr> arguments = get(var2).getArguments();
        Sort sort = getSort(ctx, arguments);
        return constructor(
                ihc(var1),
                null,
                sort,
                arguments.toArray(new Expr[0]));
    }

    public Expr constructor(Sort elementType, Expr[] arguments) {
        return constructor(UUID.randomUUID().toString().hashCode(),
                null, elementType, arguments);
    }

    private Expr constructor(int hashCode,
                            Integer capacity,
                            Sort elementType,
                            Expr[] arguments) {
        List<Expr> l;
        elementType = ctx.mkIntSort(); // TODO: fix this
        if (capacity != null) l = new ArrayList(capacity);
        else if (arguments != null) l = new ArrayList(Arrays.stream(arguments).toList());
        else l = new ArrayList();

        ArrayExpr arrayExpr = ctx.mkArrayConst("Array"+hashCode, ctx.mkIntSort(), elementType);
        for (int i = 0; i < l.size(); i++)
            ctx.mkStore(arrayExpr, ctx.mkInt(i), l.get(i));
//        Expr arrayExpr = ctx.mkConst("Array"+hashCode, SortType.ARRAY.value(ctx));

        stack.add(hashCode, new ListModel(arrayExpr, elementType, l, getSentinel(elementType)));
        return arrayExpr;
    }

    public Expr sizeExpr(Expr var1) {
        ListModel listModel = get(var1);
        return sizeExpr(listModel);

//        Optional<ListModel> listModel = stack.get(hashCode);
//        if (listModel.isEmpty()) return ctx.mkInt(0);
//        else return ctx.mkInt(listModel.get().getArguments().size());
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkBool(size(var1) == 0);
    }

    public BoolExpr add(Expr var1, Expr element) {
        ListModel listModel = get(var1);
//        Expr originalSize = sizeExpr(var1);
//        int size = size(var1);
//        Sort sort = getSort(ctx, element);
//        if (!listModel.getSort().equals(sort))

        ArrayExpr array = listModel.getExpr();
        array = ctx.mkStore(array, ctx.mkInt(listModel.getArguments().size()), element);
        listModel.setExpr(array);

        listModel.getArguments().add(element);
//        int newSize = size(var1);
//        Expr newSize = sizeExpr(var1);
//        return ctx.mkGt(newSize, originalSize);
        return ctx.mkBool(true);
//        return ctx.mkBool(newSize != size);
    }

    public BoolExpr add(Expr var1, int index, Expr var2) {
        int hashCode = initList(var1);
        ListModel listModel = stack.get(hashCode).orElseThrow();
        int size = listModel.getArguments().size();
        if (index < 0 || index > size)
            return ctx.mkBool(false);

        listModel.getArguments().add(index, var2);
//        ctx.mkStore(listModel.getExpr(), ctx.mkInt(index), var2);

        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr addAll(Expr var1, Expr var2) {
        initList(var1);
        int size = size(var1);
        List<Expr> elements = get(var2).getArguments();
        for (Expr expr : elements) add(var1, expr);
        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr addAll(Expr var1, int index, Expr var2) {
        initList(var1);
        int size = size(var1);
        List<Expr> elements = get(var2).getArguments();
        for (int idx = index, i = 0; idx < index+elements.size(); idx++, i++)
            add(var1, idx, elements.get(i));
        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr remove(Expr var1, Integer... indices) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Set<Integer> idx = new HashSet<>(Arrays.stream(indices).toList());
        for (int i = listModel.get().getArguments().size()-1; i >= 0; i--)
            if (idx.contains(i))
                listModel.get().getArguments().remove(i);

        int size = size(var1);
//        constructor(hashCode,
//                null,
//                listModel.getSort(),
//                listModel.getArguments().toArray(new Expr[0]));

        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
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
        for (int i = 0; i < targetModel.getArguments().size(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            found = ctx.mkOr(found, remove(listModel.get(), currentValue, true));
        }
        return found;
    }

    public BoolExpr contains(Expr var1, Expr element) { // X
        return containsElements(var1, null, element);
    }

    public BoolExpr containsAll(Expr var1, Expr var2) { // X
        Optional<ListModel> listModel = getOptional(var2);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        Expr[] expressions = getArguments(listModel.get());
        return containsElements(var1, listModel.get().getSentinel(), expressions);
    }

    public BoolExpr retainAll(Expr var1, Expr var2) {
        int size = size(var1);
        List<Expr> elements = get(var2).getArguments();
        for (Expr expr : get(var1).getArguments())
            if (!elements.contains(expr))
                remove(var1, expr);
        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
    }

    public Expr clear(Expr var1) {
        ListModel listModel = get(var1);
        listModel.getArguments().clear();
        return listModel.getExpr();
//        return constructor(ihc(var1),
//                null,
//                listModel.getSort(),
//                null);
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        if (stack.get(ihc(var1)).isEmpty()) return ctx.mkBool(false);
        List<Expr> elements = get(var1).getArguments();
        List<Expr> exprs = get(var2).getArguments();
        if (elements.size() != exprs.size()) return ctx.mkBool(false);
        Expr[] result = new Expr[elements.size()];
        for (int i = 0; i < elements.size(); i++)
            result[i] = ctx.mkEq(elements.get(i), exprs.get(i));
        return ctx.mkAnd(result);
    }

    public Expr get(Expr var1, int index) {
        List<Expr> elements = get(var1).getArguments();
        if (index < 0 || index >= elements.size()) throw new RuntimeException("index out of bounds");
        return elements.get(index);
    }

    public Expr set(Expr var1, int index, Expr element) {
        if (stack.get(ihc(var1)).isPresent()) {
            remove(var1, index);
            add(var1, index, element);
        }
        return element;
    }

    public Expr hashCode(Expr var1) {
        return ctx.mkInt(get(var1).getArguments().hashCode());
    }

    public Expr subList(Expr var1, int fromIndex, int toIndex) {
        ListModel listModel = get(var1);
        List<Expr> arguments = listModel.getArguments().subList(fromIndex, toIndex);
        return constructor(ihc(var1)+ (int)(Math.random()*10000),
                null,
                getSort(ctx, arguments),
                arguments.toArray(new Expr[0]));
    }

    public Expr indexOf(Expr var1, Expr element) {
        return ctx.mkInt(get(var1).getArguments().indexOf(element));
    }

    public Expr lastIndexOf(Expr var1, Expr element) {
        return ctx.mkInt(get(var1).getArguments().lastIndexOf(element));
    }

    private int size(Expr var1) {
        return Integer.parseInt(sizeExpr(var1).toString());
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

    private Expr getSentinel(Sort sort) {
//        return ctx.mkInt(-1);
        return ctx.mkConst("null", sort);
    }

    private Expr sizeExpr(ListModel listModel) {
        ArithExpr newSize = ctx.mkInt(0);
        for (int i = 0; i < listModel.getArguments().size(); i++) {
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
        for (int i = 0; i < listModel.getArguments().size(); i++) {
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

    private BoolExpr containsElements(Expr var1, Expr elementSentinel, Expr... elements) {
        if (elements == null) return ctx.mkBool(false);

        Optional<ListModel> listModel = getOptional(var1);
        if (listModel.isEmpty()) return ctx.mkBool(false);

        Expr sentinel = listModel.get().getSentinel();
        ArrayExpr array = listModel.get().getExpr();
        Expr count = ctx.mkInt(0);
        for (int i = 0; i < listModel.get().getArguments().size(); i++) {
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
        Expr[] expressions = new Expr[listModel.getArguments().size()];
        ArrayExpr array = listModel.getExpr();
        for (int i = 0; i < listModel.getArguments().size(); i++) {
            Expr currentValue = ctx.mkSelect(array, ctx.mkInt(i));
            expressions[i] = currentValue;
        }
        return expressions;
    }

    private static int ihc(Object o) {
        if (o instanceof Expr expr && SortType.ARRAY.equals(expr.getSort())) {
            try {
                return Integer.parseInt(o.toString().replace("Array", ""));
            } catch (NumberFormatException ignored) {}
        }
        return System.identityHashCode(o);
    }
}
