package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.github.a1k28.evoc.core.z3extended.model.ListModel;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.microsoft.z3.*;

import java.util.function.Function;

public class Z3ListInstance implements IStack {

    private final Context ctx;
    private final Z3CachingFactory sortState;
    private final Z3SortUnion sortUnion;
    private final Z3Stack<Expr, ListModel> stack;
    private final Z3Stack<IntExpr, Expr> discoveredValues;
    private final Z3ExtendedSolver solver;

    public Z3ListInstance(Context context,
                          Z3ExtendedSolver solver,
                          Z3CachingFactory sortState,
                          Z3SortUnion sortUnion) {
        this.ctx = context;
        this.solver = solver;
        this.sortState = sortState;
        this.sortUnion = sortUnion;
        this.stack = new Z3Stack<>();
        this.discoveredValues = new Z3Stack<>();
    }

    @Override
    public void push() {
        stack.push();
        discoveredValues.push();
    }

    @Override
    public void pop() {
        stack.pop();
        discoveredValues.pop();
    }

    public Expr constructor(Expr var1) {
        return constructor(var1, null, null, false).getReference();
    }

//    public Expr constructor(Expr var1,
//                            Integer capacity) {
//    }
//
//    public Expr constructor(Expr var1, Expr var2) {
//    }

    private ListModel constructor(Expr reference,
                             IntExpr capacity,
                             Expr[] arguments,
                             boolean isSizeUnknown) {
        Sort elementType = sortUnion.getGenericSort();
        TupleSort valueSort = sortState.mkListSort(elementType);
        Expr sentinel = sortState.mkListSentinel(elementType);

        ArrayExpr array;
        IntExpr size;
        if (isSizeUnknown) {
            size = (IntExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            array = mkArray(valueSort);

            // size assertion
            BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
            solver.add(sizeAssertion);
        } else {
            size = ctx.mkInt(0);
            array = mkEmptyArray(sentinel);
        }

//        if (arguments != null)
//            for (Expr argument : arguments)
//                add(arrayExpr, argument);

        ListModel listModel = new ListModel(
                reference, array, valueSort, sentinel, size, capacity, isSizeUnknown);
        stack.add(reference, listModel);
        return listModel;
    }

    private ArrayExpr mkArray(Sort valueSort) {
        ArraySort arraySort = ctx.mkArraySort(ctx.mkIntSort(), valueSort);
        return (ArrayExpr) ctx.mkFreshConst("Array", arraySort);
    }

    private ArrayExpr mkEmptyArray(Expr sentinel) {
        return ctx.mkConstArray(ctx.mkIntSort(), sentinel);
    }

//    public Expr size(Expr var1) {
//        ListModel listModel = get(var1);
//        return size(listModel);
//    }

//    public BoolExpr isEmpty(Expr var1) {
//        return ctx.mkEq(size(var1), ctx.mkInt(0));
//    }

//    public Expr size(MapModel model) {
//        return model.getSize();
//    }

    public BoolExpr add(Expr var1, Expr element) {
        ListModel model = copyModel(getModel(var1));
        return add(model, element);
    }

    public Expr add(Expr var1, IntExpr index, Expr element) {
        ListModel model = copyModel(getModel(var1));
        IntExpr size = model.getSize();
        BoolExpr response = add(model, element);
//        FuncDecl<IntSort> mappingFunc = ctx.mkFreshFuncDecl(
//                "idxMap", new Sort[]{ctx.mkIntSort()}, ctx.mkIntSort());
//        BoolExpr indexCondition = ctx.mkEq(size, ctx.mkApp(mappingFunc, index));

        Function<IntExpr, IntExpr> mappingFunc = (IntExpr x) -> {
            BoolExpr idxCondition = ctx.mkAnd(response, ctx.mkEq(x, index));
            BoolExpr shiftCondition = ctx.mkAnd(response, ctx.mkGt(x, index), ctx.mkLe(x, size));
            IntExpr shiftExpr = (IntExpr) ctx.mkITE(shiftCondition,
                    ctx.mkSub(x, ctx.mkInt(1)),
                    x);
            return (IntExpr) ctx.mkITE(idxCondition, size, shiftExpr);
        };

        model.getIndexMaps().add(mappingFunc);
        return null;
    }

//    public BoolExpr addAll(Expr var1, Expr var2) {
//    }
//
//    public BoolExpr addAll(Expr var1, ArithExpr index, Expr var2) {
//    }
//
//    public Expr remove(Expr var1, IntExpr index) {
//    }
//
//    // remove the first occurrence of the element
//    public BoolExpr remove(Expr var1, Expr element) {
//    }
//
//    public BoolExpr removeAll(Expr var1, Expr var2) {
//    }
//
//    public BoolExpr contains(Expr var1, Expr element) {
//        return containsElements(var1, element);
//    }
//
//    public BoolExpr containsAll(Expr var1, Expr var2) {
//    }
//
//    public BoolExpr retainAll(Expr var1, Expr var2) {
//    }
//
//    public Expr clear(Expr var1) {
//    }
//
//    public BoolExpr equals(Expr var1, Expr var2) {
//    }
//
    public Expr get(Expr var1, IntExpr index) {
        ListModel model = getModel(var1);
        IntExpr mappedIndex = mapIndex(model, index);
        Expr retrieved = ctx.mkSelect(model.getArray(), mappedIndex);
        return sortUnion.unwrapValue(model.getValue(retrieved), null);
    }
//
//    public Expr set(Expr var1, IntExpr index, Expr element) {
//    }
//
//    // TODO: what to do here?
//    public Expr hashCode(Expr var1) {
//    }
//
//    public Expr subList(Expr var1, IntExpr fromIndex, IntExpr toIndex) {
//    }
//
//    public Expr indexOf(Expr var1, Expr element) {
//    }
//
//    public Expr lastIndexOf(Expr var1, Expr element) {
//    }

//    private void shiftRight(ListModel model, ArithExpr index) {
//        ArithExpr size = model.getSize();
//
//        size = (ArithExpr) ctx.mkITE(
//                ctx.mkLt(index, size),
//                ctx.mkAdd(size, ctx.mkInt(1)),
//                size);
//
//        model.setSize(size);
//        if (model.isSizeUnknown())
//            return;
//
//        ArrayExpr array = model.getArray();
//        IntExpr i = ctx.mkIntConst("i");
//        BoolExpr condition = ctx.mkAnd(
//                ctx.mkGt(i, index),
//                ctx.mkLe(i, size)
//        );
//        // Shift elements to the right
//        array = (ArrayExpr) ctx.mkITE(
//                condition,
//                ctx.mkStore(array, i, ctx.mkSelect(array, ctx.mkSub(i, ctx.mkInt(1)))),
//                array
//        );
//
//        model.setArray(array);
//    }

    private BoolExpr add(ListModel model, Expr element) {
        ArrayExpr array = model.getArray();
        IntExpr size = model.getSize();
        BoolExpr listWasModified;
        Expr valueWrapped = sortUnion.wrapValue(element);
        Expr valueToBeSaved = model.mkDecl(valueWrapped, ctx.mkFalse());

        if (model.getCapacity() == null) {
            listWasModified = ctx.mkTrue();
            array = ctx.mkStore(array, size, valueToBeSaved);
            size = (IntExpr) ctx.mkAdd(size, ctx.mkInt(1));
        } else {
            ArrayExpr newArr = ctx.mkStore(array, size, valueToBeSaved);
            listWasModified = ctx.mkGt(model.getCapacity(), size);
            array = (ArrayExpr) ctx.mkITE(listWasModified, newArr, array);
            size = (IntExpr) ctx.mkITE(listWasModified, ctx.mkAdd(size, ctx.mkInt(1)), size);
        }

        model.setArray(array);
        model.setSize(size);
        return listWasModified;
    }

    private BoolExpr exists(ListModel model, ArithExpr index) {
        Expr retrieved = ctx.mkSelect(model.getArray(), index);
        return ctx.mkNot(model.isEmpty(retrieved));
    }

    private ListModel getModel(Expr var1) {
        return getModel(var1, true);
    }

    private ListModel getModel(Expr expr, boolean isSizeUnknown) {
        if (stack.get(expr).isEmpty())
            return createModel(expr, isSizeUnknown);
        return stack.get(expr).orElseThrow();
    }

    private ListModel createModel(Expr expr, boolean isSizeUnknown) {
        ListModel listModel = constructor(expr, null, null, isSizeUnknown);
        stack.add(expr, listModel);
        return listModel;
    }

    private ListModel copyModel(ListModel model) {
        ListModel newModel = new ListModel(model);
        stack.add(newModel.getReference(), newModel);
        return newModel;
    }

    private IntExpr mapIndex(ListModel listModel, IntExpr index) {
        for (int k = listModel.getIndexMaps().size() - 1; k >= 0; k--) {
//            index = (IntExpr) ctx.mkApp(listModel.getIndexMaps().get(k), index);
            index = listModel.getIndexMaps().get(k).apply(index);
        }
        return index;
    }
}
