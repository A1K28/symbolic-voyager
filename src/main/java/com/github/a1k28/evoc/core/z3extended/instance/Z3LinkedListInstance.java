package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.github.a1k28.evoc.core.z3extended.model.LinkedListModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.microsoft.z3.*;

public class Z3LinkedListInstance implements IStack {

    private final Z3ExtendedContext ctx;
    private final Z3SortUnion sortUnion;
    private final Z3Stack<Expr, LinkedListModel> stack;
    private final Z3ExtendedSolver solver;
    private final TupleSort nodeSort;
    private ArrayExpr referenceMap;
    private final FuncDecl searchRecFunc;
    private final Sort referenceSort;
    private Expr refCounter;

    public Z3LinkedListInstance(Z3ExtendedContext context,
                                Z3ExtendedSolver solver,
                                Z3CachingFactory sortState,
                                Z3SortUnion sortUnion) {
        this.ctx = context;
        this.solver = solver;
        this.sortUnion = sortUnion;
        this.stack = new Z3Stack<>();

        Sort elementType = sortUnion.getGenericSort();
        this.nodeSort = sortState.mkLinkedListSort(elementType);
//        this.referenceSort = SortType.REFERENCE.value(ctx);
        this.referenceSort = ctx.mkIntSort();
        this.referenceMap = mkArray();
        this.refCounter = ctx.mkInt(0);

        this.searchRecFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("SearchRecFunc"),
                new Sort[]{referenceSort, ctx.mkIntSort(), ctx.mkIntSort(), ctx.mkArraySort(referenceSort, nodeSort)},
                referenceSort);
        defineSearchFunc();
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
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

    private LinkedListModel constructor(Expr reference,
                             IntExpr capacity,
                             Expr[] arguments,
                             boolean isSizeUnknown) {
        IntExpr size;
//        Expr headRef = ctx.mkFreshConst("headRef", referenceSort);
//        Expr tailRef = ctx.mkFreshConst("tailRef", referenceSort);
        Expr headRef = mkNextRef();
        Expr tailRef = mkNextRef();

        Expr nill = sortUnion.wrapValue(ctx.mkNull());

        Expr<TupleSort> head;
        Expr<TupleSort> tail;
        if (isSizeUnknown) {
            size = (IntExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            // size assertion
            BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
            solver.add(sizeAssertion);

            Expr firstRef = ctx.mkFreshConst("firstRef", referenceSort);
            Expr lastRef = ctx.mkFreshConst("lastRef", referenceSort);

            head = mkDecl(nill, headRef, firstRef, headRef);
            tail = mkDecl(nill, tailRef, tailRef, lastRef);
        } else {
            size = ctx.mkInt(0);

            head = mkDecl(nill, headRef, tailRef, headRef);
            tail = mkDecl(nill, tailRef, tailRef, headRef);
        }

        referenceMap = ctx.mkStore(referenceMap, headRef, head);
        referenceMap = ctx.mkStore(referenceMap, tailRef, tail);

        LinkedListModel linkedListModel = new LinkedListModel(
                reference, headRef, tailRef, size, capacity, isSizeUnknown);

//        if (arguments != null)
//            for (Expr argument : arguments)
//                add(listModel, argument);

        stack.add(reference, linkedListModel);
        return linkedListModel;
    }

    public BoolExpr add(Expr var1, Expr element) {
        LinkedListModel model = copyModel(getModel(var1));
        return addBeforeReference(model, model.getTailReference(), element);
    }

    public Expr add(Expr var1, IntExpr index, Expr element) {
        LinkedListModel model = copyModel(getModel(var1));
        Expr ref = getReferenceByIndex(model.getHeadReference(), index);
        return addBeforeReference(model, ref, element);
    }

//    public BoolExpr addAll(Expr var1, Expr var2) {
//        ListModel target = copyModel(getModel(var1));
//        ListModel source = getModel(var2);
//
//        ArrayExpr targetArray = target.getArray();
//        ArrayExpr sourceArray = source.getArray();
//
//        IntExpr i = ctx.mkIntConst("i");
//        BoolExpr isSourceIdxEmpty = isValEmpty(ctx.mkSelect(sourceArray, i));
//        BoolExpr body = ctx.mkAnd(
//                ctx.mkLe(ctx.mkInt(0), i),
//                ctx.mkLt(i, source.getInternalSize()),
//                ctx.mkEq(
//                        targetArray,
//                        ctx.mkStore(targetArray,
//                                ctx.mkAdd(i, target.getInternalSize()),
//                                ctx.mkSelect(sourceArray, i))
//                )
//        );
//
//        target.setSize((IntExpr) ctx.mkAdd(target.getSize(), source.getSize()));
//        target.setInternalSize((IntExpr) ctx.mkAdd(target.getInternalSize(), source.getInternalSize()));
//
//        return ctx.mkTrue();
//    }

//
//    public BoolExpr addAll(Expr var1, ArithExpr index, Expr var2) {
//    }

//    public Expr remove(Expr var1, IntExpr index) {
//        ListModel model = copyModel(getModel(var1));
//        ArrayExpr array = model.getArray();
//        IntExpr size = model.getSize();
//        IntExpr mappedIndex = mapIndex(model, index);
//
//        Expr retrieved = ctx.mkSelect(model.getArray(), mappedIndex);
//        Expr unwrapped = sortUnion.unwrapValue(getValue(retrieved), ctx.mkNull());
//
//        Tuple<Function<IntExpr, IntExpr>> mappingFunctions = createLeftShiftMappings(index);
//        model.getIndexMaps().add(mappingFunctions);
//
//        array = ctx.mkStore(array, mappedIndex, sentinel);
//        size = (IntExpr) ctx.mkSub(size, ctx.mkInt(1));
//
//        model.setArray(array);
//        model.setSize(size);
//
//        return unwrapped;
//    }

    // remove the first occurrence of the element
//    public BoolExpr remove(Expr var1, Expr element) {
//        ListModel model = getModel(var1);
//    }

//    private BoolExpr existsByValue(ArrayExpr array, Expr value, IntExpr maxIndexExclusive) {
//        IntExpr index = (IntExpr) ctx.mkFreshConst("index", ctx.mkIntSort());
//        BoolExpr existsCondition = existsByValueCondition(array, index, value);
//        BoolExpr existsMatch = ctx.mkExists(
//                new Expr[]{index},
//                ctx.mkAnd(ctx.mkLt(index, maxIndexExclusive), existsCondition),
//                1, null, null, null, null
//        );
//        solver.add(ctx.mkImplies(existsMatch, existsCondition));
//    }

//    public BoolExpr removeAll(Expr var1, Expr var2) {
//    }
//
//    public BoolExpr contains(Expr var1, Expr element) {
//        ListModel model = getModel(var1);
//        return contains(model, sortUnion.wrapValue(element));
//    }
//
//    public BoolExpr containsAll(Expr var1, Expr var2) {
//        ListModel model1 = getModel(var1);
//        ListModel model2 = getModel(var2);
//
//        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.mkIntSort());
//
//        Expr retrieved = ctx.mkSelect(model2.getArray(), i);
//        Expr wrappedValue = getValue(retrieved);
//        Expr existsInSource = ctx.mkNot(isValEmpty(retrieved));
//
//        return ctx.mkForall(
//                new Expr[]{i},
//                ctx.mkImplies(existsInSource, contains(model1, wrappedValue)),
//                1, null, null, null, null);
//    }

//    public BoolExpr retainAll(Expr var1, Expr var2) {
//    }
//
//    public Expr clear(Expr var1) {
//        ListModel model = copyModel(getModel(var1));
//        ArrayExpr updatedArray = mkEmptyArray();
//        model.setArray(updatedArray);
//        IntExpr size = ctx.mkInt(0);
//        model.setSize(size);
//        model.setInternalSize(size);
//        model.setSizeUnknown(false);
//        return null;
//    }
//
//    public BoolExpr equals(Expr var1, Expr var2) {
//    }
//
    public Expr get(Expr var1, IntExpr index) {
        LinkedListModel model = getModel(var1);
        Expr ref = getReferenceByIndex(model.getHeadReference(), index);
        Expr retrieved = ctx.mkSelect(referenceMap, ref);
        return sortUnion.unwrapValue(getValue(retrieved), ctx.mkNull());
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
//        ListModel model = copyModel(getModel(var1));
//        Expr reference = ctx.mkFreshConst("var2", var1.getSort());
//        model.setReference(reference);
//
//        ArrayExpr sublist = mkArray();
//
//        IntExpr i = (IntExpr) ctx.mkFreshConst("i", ctx.mkIntSort());
//        Expr retrieved = ctx.mkSelect(model.getArray(), i);
//        Expr wrappedValue = getValue(retrieved);
//        Expr existsInSource = ctx.mkNot(isValEmpty(retrieved));
//        BoolExpr existsInTargetCondition = existsByValueCondition(
//                sublist, mapIndex(model, i), wrappedValue);
//
//        BoolExpr sublistDef = ctx.mkForall(
//                new Expr[]{i},
//                ctx.mkImplies(
//                        ctx.mkAnd(
//                                ctx.mkGe(i, ctx.mkInt(0)),
//                                ctx.mkLt(i, ctx.mkSub(toIndex, fromIndex))
//                        ),
//                        ctx.mkEq(
//                                ctx.mkSelect(sublist, i),
//                                ctx.mkSelect(model.getArray(), ctx.mkAdd(i, fromIndex))
//                        )
//                ),
//                1, null, null, null, null
//        );
//
////        BoolExpr condition = ctx.mkForall(
////                new Expr[]{i},
////                ctx.mkImplies(existsInSource, existsInTargetCondition),
////                1, null, null, null, null);
////        solver.add(condition);
//
//        model.setArray(arrayExpr);
//
//        return reference;
//    }
//
//    public Expr indexOf(Expr var1, Expr element) {
//    }

//    public Expr lastIndexOf(Expr var1, Expr element) {
//    }

    private Expr getReferenceByIndex(Expr headRef, IntExpr index) {
        Expr head = ctx.mkSelect(referenceMap, headRef);
        Expr nextRef = getNextRef(head);
        return searchRecFunc.apply(nextRef, ctx.mkInt(0), index, referenceMap);
    }

    /**
     *
     * @param model the in-memory model
     * @param nextRef the reference, which should be next to the new element
     * @param element the value
     * @return true if the list was modified by the result of this call
     */
    private BoolExpr addBeforeReference(LinkedListModel model, Expr nextRef, Expr element) {
        Expr wrappedElement = sortUnion.wrapValue(element);
        Expr lastNode = ctx.mkSelect(referenceMap, nextRef);
        Expr prevRef = getPrevRef(lastNode);
        Expr newRef = mkNextRef();

        Expr node = mkDecl(wrappedElement, newRef, nextRef, prevRef);
        referenceMap = ctx.mkStore(referenceMap, newRef, node);

        Expr prevNode = ctx.mkSelect(referenceMap, prevRef);
        Expr prevNodeUpdated = mkDecl(getValue(prevNode), prevRef, newRef, getPrevRef(prevNode));
        referenceMap = ctx.mkStore(referenceMap, prevRef, prevNodeUpdated);

        IntExpr size = (IntExpr) ctx.mkAdd(model.getSize(), ctx.mkInt(1));
        Expr nextNodeUpdated = mkDecl(getValue(lastNode), nextRef, getNextRef(lastNode), newRef);
        referenceMap = ctx.mkStore(referenceMap, nextRef, nextNodeUpdated);

        model.setSize(size);
        return ctx.mkTrue();
    }

    private LinkedListModel getModel(Expr var1) {
        return getModel(var1, true);
    }

    private LinkedListModel getModel(Expr expr, boolean isSizeUnknown) {
        if (stack.get(expr).isEmpty())
            return createModel(expr, isSizeUnknown);
        return stack.get(expr).orElseThrow();
    }

    private LinkedListModel createModel(Expr expr, boolean isSizeUnknown) {
        LinkedListModel listModel = constructor(expr, null, null, isSizeUnknown);
        stack.add(expr, listModel);
        return listModel;
    }

    private LinkedListModel copyModel(LinkedListModel model) {
        LinkedListModel newModel = new LinkedListModel(model);
        stack.add(newModel.getReference(), newModel);
        return newModel;
    }

    private Expr mkDecl(Expr element, Expr ref, Expr nextRef, Expr prevRef) {
        return this.nodeSort.mkDecl().apply(element, ref, nextRef, prevRef);
    }

    private Expr getValue(Expr node) {
        return this.nodeSort.getFieldDecls()[0].apply(node);
    }

    private Expr getRef(Expr node) {
        return this.nodeSort.getFieldDecls()[1].apply(node);
    }

    private Expr getNextRef(Expr node) {
        return this.nodeSort.getFieldDecls()[2].apply(node);
    }

    private Expr getPrevRef(Expr node) {
        return this.nodeSort.getFieldDecls()[3].apply(node);
    }

    // assuming index out of bounds does not occur!
    private void defineSearchFunc() {
        Expr ref = ctx.mkBound(0, referenceSort);
        Expr i = ctx.mkBound(1, ctx.mkIntSort());
        Expr index = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = getNextRef(node);

        BoolExpr isLegalState = ctx.mkAnd(
                ctx.mkGe(i, ctx.mkInt(0)),
                ctx.mkLt(i, index));

        BoolExpr condition = ctx.mkEq(i, index);
        IntExpr iPlusOne = (IntExpr) ctx.mkAdd(i, ctx.mkInt(1));

        Expr baseCase = ref;
        Expr recursiveCase = searchRecFunc.apply(
                nextRef,
                iPlusOne,
                index,
                referenceMap);

        Expr body = ctx.mkITE(
                condition,
                baseCase,
                ctx.mkITE(isLegalState, recursiveCase, baseCase));

        ctx.AddRecDef(searchRecFunc, new Expr[]{ref, i, index, referenceMap}, body);
    }

    private ArrayExpr mkArray() {
        ArraySort arraySort = ctx.mkArraySort(referenceSort, nodeSort);
        return (ArrayExpr) ctx.mkFreshConst("Array", arraySort);
    }

    private Expr mkNextRef() {
        Expr ref = refCounter;
        refCounter = ctx.mkAdd(refCounter, ctx.mkInt(1));
        return ref;
    }

//    private ArrayExpr mkEmptyArray() {
//        return ctx.mkConstArray(referenceSort, mkNullNode());
//    }

//    private Expr mkNullReference() {
//        return ctx.mkConst("null", referenceSort);
//    }

//    private Expr mkNullNode() {
//        Expr wrappedNull = sortUnion.wrapValue(ctx.mkNull());
//        Expr nullReference = mkNullReference();
//        return this.nodeSort.mkDecl().apply(
//                wrappedNull, nullReference, nullReference, nullReference);
//    }
}
