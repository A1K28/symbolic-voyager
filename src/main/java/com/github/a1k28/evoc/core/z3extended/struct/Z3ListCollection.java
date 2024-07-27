package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.ListModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.*;

import static com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext.getSort;

public class Z3ListCollection implements IStack {
    private final Context ctx;
    private final Z3Stack<Integer, ListModel> stack; // TODO: stack struct?

    @Override
    public void push() {
    }

    @Override
    public void pop() {
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
        if (capacity != null) l = new ArrayList(capacity);
        else if (arguments != null) l = new ArrayList(Arrays.stream(arguments).toList());
        else l = new ArrayList();

//        ArrayExpr arrayExpr = ctx.mkArrayConst("ArrayList"+hashCode, ctx.mkIntSort(), elementType);
//        for (int i = 0; i < l.size(); i++)
//            ctx.mkStore(arrayExpr, ctx.mkInt(i), l.get(i));
        Expr arrayExpr = ctx.mkConst("ArrayList"+hashCode, SortType.ARRAY.value(ctx));

        stack.add(hashCode, new ListModel(arrayExpr, elementType, l));
        return arrayExpr;
    }

    public Expr sizeExpr(Expr var1) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkInt(0);
        else return ctx.mkInt(listModel.get().getArguments().size());
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkBool(size(var1) == 0);
    }

    public BoolExpr add(Expr var1, Expr element) {
        int hashCode = initList(var1);
        int size = size(var1);
        ListModel listModel = stack.get(hashCode).orElseThrow();
//        Sort sort = getSort(ctx, element);
//        if (!listModel.getSort().equals(sort))
        listModel.getArguments().add(element);
//        ctx.mkStore(listModel.getExpr(), ctx.mkInt(listModel.getArguments().size()), element);
        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
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

    public BoolExpr remove(Expr var1, Expr element) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        int index = listModel.get().getArguments().indexOf(element);
        return remove(var1, index);
    }

    public BoolExpr removeAll(Expr var1, Expr var2) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        List<Expr> elements = get(var2).getArguments();
        Integer[] indices = new Integer[elements.size()];
        for (int i = 0; i < elements.size(); i++)
            indices[i] = listModel.get().getArguments().indexOf(elements.get(i));
        int size = size(var1);
        remove(var1, indices);
        int newSize = size(var1);
        return ctx.mkBool(newSize != size);
    }

    public BoolExpr contains(Expr var1, Expr element) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        List<Expr> set = listModel.get().getArguments();
        Expr[] expressions = new Expr[set.size()];
        int i = 0;
        for (Expr value : set) {
            expressions[i] = ctx.mkEq(value, element);
            i++;
        }
        return ctx.mkOr(expressions);
    }

    public BoolExpr containsAll(Expr var1, Expr var2) {
        int hashCode = ihc(var1);
        Optional<ListModel> listModel = stack.get(hashCode);
        if (listModel.isEmpty()) return ctx.mkBool(false);
        List<Expr> set = listModel.get().getArguments();
        List<Expr> elements = get(var2).getArguments();
        Expr[] expressions = new Expr[elements.size()];
        int i = 0;
        for (Expr element : elements) {
            Expr[] exps = new Expr[set.size()];
            int j = 0;
            for (Expr se : set) {
                exps[j] = ctx.mkEq(se, element);
                j++;
            }
            expressions[i] = ctx.mkOr(exps);
            i++;
        }
        return ctx.mkAnd(expressions);
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
        return get(var1).getArguments().size();
    }

    private ListModel get(Expr var1) {
        int hashCode = initList(var1);
        return stack.get(hashCode).orElseThrow();
    }

    private int initList(Expr var1) {
        int hashCode = ihc(var1);
        if (stack.get(hashCode).isEmpty())
            constructor(var1);
        return hashCode;
    }

    public static int ihc(Object o) {
        if (o instanceof Expr expr && SortType.ARRAY.equals(expr.getSort())) {
            try {
                return Integer.parseInt(o.toString().replace("ArrayList", ""));
            } catch (NumberFormatException ignored) {}
        }
        return System.identityHashCode(o);
    }
}
