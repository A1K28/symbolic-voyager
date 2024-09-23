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
    private final Sort valueSort;
    private final TupleSort nodeSort;
    private final Sort referenceSort;

    private FuncDecl searchRecFunc;
    private FuncDecl findRefByValueFunc;
    private FuncDecl findIdxByValueFunc;
    private FuncDecl findLastIdxByValueFunc;
    private FuncDecl<SetSort> toSetFunc;
    private FuncDecl<BoolSort> containsAllFunc;
    private FuncDecl<BoolSort> equalsFunc;

    public Z3LinkedListInstance(Z3ExtendedContext context,
                                Z3ExtendedSolver solver,
                                Z3CachingFactory sortState,
                                Z3SortUnion sortUnion) {
        this.ctx = context;
        this.solver = solver;
        this.sortUnion = sortUnion;
        this.stack = new Z3Stack<>();

        this.valueSort = sortUnion.getGenericSort();
        this.referenceSort = ctx.mkIntSort();
        this.nodeSort = sortState.mkLinkedListSort(referenceSort, valueSort);

        defineSearchFunc();
        defineFindRefByValueFunc();
        defineFindIdxByValueFunc();
        defineFindLastIdxByValueFunc();
        defineToSetFunc();
        defineContainsAllFunc();
        defineEqualsFunc();
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
        IntExpr refCounter = ctx.mkInt(2);
        Expr headRef = ctx.mkInt(0);
        Expr tailRef = ctx.mkInt(1);

        Expr nill = sortUnion.wrapValue(ctx.mkNull());

        Expr<TupleSort> head;
        Expr<TupleSort> tail;
        ArrayExpr referenceMap;
        if (isSizeUnknown) {
            referenceMap = mkArray();
            size = (IntExpr) ctx.mkFreshConst("size", ctx.mkIntSort());
            // size assertion
            BoolExpr sizeAssertion = ctx.mkGe(size, ctx.mkInt(0));
            solver.add(sizeAssertion);

            Expr firstRef = ctx.mkFreshConst("firstRef", referenceSort);
            Expr lastRef = ctx.mkFreshConst("lastRef", referenceSort);

            head = mkDecl(nill, headRef, firstRef, headRef);
            tail = mkDecl(nill, tailRef, tailRef, lastRef);
        } else {
            referenceMap = mkEmptyArray();
            size = ctx.mkInt(0);

            head = mkDecl(nill, headRef, tailRef, headRef);
            tail = mkDecl(nill, tailRef, tailRef, headRef);
        }

        referenceMap = ctx.mkStore(referenceMap, headRef, head);
        referenceMap = ctx.mkStore(referenceMap, tailRef, tail);

        LinkedListModel linkedListModel = new LinkedListModel(
                reference, referenceMap, headRef, tailRef, size, capacity, refCounter, isSizeUnknown);

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
        Expr ref = getReferenceByIndex(model.getReferenceMap(), model.getHeadReference(), index);
        return addBeforeReference(model, ref, element);
    }

    public IntExpr size(Expr var1) {
        return getModel(var1).getSize();
    }

    public BoolExpr isEmpty(Expr var1) {
        return ctx.mkEq(getModel(var1).getSize(), ctx.mkInt(0));
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

    public Expr remove(Expr var1, IntExpr index) {
        LinkedListModel model = copyModel(getModel(var1));
        Expr ref = getReferenceByIndex(model.getReferenceMap(), model.getHeadReference(), index);
        return remove(model, ref);
    }

    // remove the first occurrence of the element
    public BoolExpr remove(Expr var1, Expr element) {
        LinkedListModel model = getModel(var1);
        IntExpr oldSize = model.getSize();
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr head = ctx.mkSelect(referenceMap, model.getHeadReference());
        Expr nextRef = getNextRef(head);
        Expr ref = findRefByValueFunc.apply(
                nextRef, sortUnion.wrapValue(element), referenceMap);
        remove(model, ref);
        IntExpr newSize = model.getSize();
        return ctx.mkLt(newSize, oldSize);
    }

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
    public BoolExpr contains(Expr var1, Expr element) {
        return ctx.mkNot(ctx.mkEq(indexOf(var1, element), ctx.mkInt(-1)));
    }

    public BoolExpr containsAll(Expr var1, Expr var2) {
        LinkedListModel model2 = getModel(var1);
        LinkedListModel model1 = getModel(var2);

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        ArrayExpr set = (ArrayExpr) toSetFunc.apply(
                getNextRef(head2), ctx.mkEmptySet(referenceSort), model2.getReferenceMap());
        return (BoolExpr) containsAllFunc.apply(getNextRef(head1), set, model1.getReferenceMap());
    }

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
    public Expr clear(Expr var1) {
        LinkedListModel model = copyModel(getModel(var1));

        Expr nill = sortUnion.wrapValue(ctx.mkNull());
        Expr headRef = model.getHeadReference();
        Expr tailRef = model.getTailReference();

        ArrayExpr referenceMap = mkEmptyArray();
        IntExpr size = ctx.mkInt(0);

        Expr head = mkDecl(nill, headRef, tailRef, headRef);
        Expr tail = mkDecl(nill, tailRef, tailRef, headRef);

        referenceMap = ctx.mkStore(referenceMap, headRef, head);
        referenceMap = ctx.mkStore(referenceMap, tailRef, tail);

        model.setReferenceMap(referenceMap);
        model.setSize(size);
        model.setSizeUnknown(false);
        return null;
    }

    public Expr get(Expr var1, IntExpr index) {
        LinkedListModel model = getModel(var1);
        Expr ref = getReferenceByIndex(model.getReferenceMap(), model.getHeadReference(), index);
        Expr retrieved = ctx.mkSelect(model.getReferenceMap(), ref);
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
    public Expr indexOf(Expr var1, Expr element) {
        LinkedListModel model = getModel(var1);
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr head = ctx.mkSelect(referenceMap, model.getHeadReference());
        Expr nextRef = getNextRef(head);
        return findIdxByValueFunc.apply(
                nextRef, sortUnion.wrapValue(element), ctx.mkInt(0), referenceMap);
    }

    public Expr lastIndexOf(Expr var1, Expr element) {
        LinkedListModel model = getModel(var1);
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr tail = ctx.mkSelect(referenceMap, model.getTailReference());
        Expr prevRef = getPrevRef(tail);
        IntExpr lastIdx = (IntExpr) findLastIdxByValueFunc.apply(
                prevRef, sortUnion.wrapValue(element), ctx.mkInt(0), referenceMap);
        return ctx.mkSub(model.getSize(), ctx.mkAdd(lastIdx, ctx.mkInt(1)));
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        LinkedListModel model1 = getModel(var1);
        LinkedListModel model2 = getModel(var2);

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        return (BoolExpr) equalsFunc.apply(getNextRef(head1), model1.getReferenceMap(),
                getNextRef(head2), model2.getReferenceMap());
    }

    private Expr getReferenceByIndex(ArrayExpr referenceMap, Expr headRef, IntExpr index) {
        Expr head = ctx.mkSelect(referenceMap, headRef);
        Expr nextRef = getNextRef(head);
        return searchRecFunc.apply(nextRef, ctx.mkInt(0), index, referenceMap);
    }

    private Expr remove(LinkedListModel model, Expr reference) {
        // can only remove the element if the ref was found
        BoolExpr canRemove = ctx.mkAnd(
                ctx.mkNot(ctx.mkEq(model.getHeadReference(), reference)),
                ctx.mkNot(ctx.mkEq(model.getTailReference(), reference))
        );

        ArrayExpr referenceMap = model.getReferenceMap();

        Expr node = ctx.mkSelect(referenceMap, reference);
        Expr prevRef = getPrevRef(node);
        Expr nextRef = getNextRef(node);

        Expr prevNode = ctx.mkSelect(referenceMap, prevRef);
        Expr nextNode = ctx.mkSelect(referenceMap, nextRef);

        Expr prevNodeUpdated = mkDecl(getValue(prevNode), prevRef, nextRef, getPrevRef(prevNode));
        Expr nextNodeUpdated = mkDecl(getValue(nextNode), nextRef, getNextRef(nextNode), prevRef);

        referenceMap = (ArrayExpr) ctx.mkITE(canRemove, ctx.mkStore(referenceMap, prevRef, prevNodeUpdated), referenceMap);
        referenceMap = (ArrayExpr) ctx.mkITE(canRemove, ctx.mkStore(referenceMap, nextRef, nextNodeUpdated), referenceMap);
        IntExpr size = (IntExpr) ctx.mkITE(canRemove, ctx.mkSub(model.getSize(), ctx.mkInt(1)), model.getSize());

        model.setSize(size);
        model.setReferenceMap(referenceMap);
        return sortUnion.unwrapValue(getValue(node), ctx.mkNull());
    }

    /**
     *
     * @param model the in-memory model
     * @param nextRef the reference, which should be next to the new element
     * @param element the value
     * @return true if the list was modified by the result of this call
     */
    private BoolExpr addBeforeReference(LinkedListModel model, Expr nextRef, Expr element) {
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr wrappedElement = sortUnion.wrapValue(element);
        Expr lastNode = ctx.mkSelect(referenceMap, nextRef);
        Expr prevRef = getPrevRef(lastNode);
        Expr newRef = mkNextRef(model);

        Expr node = mkDecl(wrappedElement, newRef, nextRef, prevRef);
        referenceMap = ctx.mkStore(referenceMap, newRef, node);

        Expr prevNode = ctx.mkSelect(referenceMap, prevRef);
        Expr prevNodeUpdated = mkDecl(getValue(prevNode), prevRef, newRef, getPrevRef(prevNode));
        referenceMap = ctx.mkStore(referenceMap, prevRef, prevNodeUpdated);

        Expr nextNodeUpdated = mkDecl(getValue(lastNode), nextRef, getNextRef(lastNode), newRef);
        referenceMap = ctx.mkStore(referenceMap, nextRef, nextNodeUpdated);

        IntExpr size = (IntExpr) ctx.mkAdd(model.getSize(), ctx.mkInt(1));

        model.setReferenceMap(referenceMap);
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
        this.searchRecFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListSearchRecFunc"),
                new Sort[]{referenceSort, ctx.mkIntSort(), ctx.mkIntSort(), ctx.mkArraySort(referenceSort, nodeSort)},
                referenceSort);

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

    private void defineFindRefByValueFunc() {
        this.findRefByValueFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFindRefByValueFunc"),
                new Sort[]{referenceSort, valueSort, ctx.mkArraySort(referenceSort, nodeSort)},
                ctx.mkIntSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr referenceMap = ctx.mkBound(2, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = getValue(node);
        Expr nextRef = getNextRef(node);

        BoolExpr isIllegalState = ctx.mkEq(ref, nextRef);
        BoolExpr condition = ctx.mkEq(value, retrievedVal);

        Expr recursiveCase = findRefByValueFunc.apply(
                nextRef,
                value,
                referenceMap);

        Expr body = ctx.mkITE(
                ctx.mkOr(condition, isIllegalState),
                ref,
                recursiveCase);

        ctx.AddRecDef(findRefByValueFunc, new Expr[]{ref, value, referenceMap}, body);
    }

    private void defineFindIdxByValueFunc() {
        this.findIdxByValueFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFindIdxByValueFunc"),
                new Sort[]{referenceSort, valueSort, ctx.mkIntSort(), ctx.mkArraySort(referenceSort, nodeSort)},
                ctx.mkIntSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr idx = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = getValue(node);
        Expr nextRef = getNextRef(node);

        BoolExpr isIllegalState = ctx.mkEq(ref, nextRef);
        BoolExpr condition = ctx.mkEq(value, retrievedVal);

        Expr baseCase = idx;
        Expr recursiveCase = findIdxByValueFunc.apply(
                nextRef,
                value,
                ctx.mkAdd(idx, ctx.mkInt(1)),
                referenceMap);

        Expr body = ctx.mkITE(
                condition,
                baseCase,
                ctx.mkITE(isIllegalState, ctx.mkInt(-1), recursiveCase));

        ctx.AddRecDef(findIdxByValueFunc, new Expr[]{ref, value, idx, referenceMap}, body);
    }

    private void defineFindLastIdxByValueFunc() {
        this.findLastIdxByValueFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFindLastIdxByValueFunc"),
                new Sort[]{referenceSort, valueSort, ctx.mkIntSort(), ctx.mkArraySort(referenceSort, nodeSort)},
                ctx.mkIntSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr idx = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = getValue(node);
        Expr prevRef = getPrevRef(node);

        BoolExpr isIllegalState = ctx.mkEq(ref, prevRef);
        BoolExpr condition = ctx.mkEq(value, retrievedVal);

        Expr baseCase = idx;
        Expr recursiveCase = findLastIdxByValueFunc.apply(
                prevRef,
                value,
                ctx.mkAdd(idx, ctx.mkInt(1)),
                referenceMap);

        Expr body = ctx.mkITE(
                condition,
                baseCase,
                ctx.mkITE(isIllegalState, ctx.mkInt(-1), recursiveCase));

        ctx.AddRecDef(findLastIdxByValueFunc, new Expr[]{ref, value, idx, referenceMap}, body);
    }

    private void defineToSetFunc() {
        this.toSetFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListToSetFunc"),
                new Sort[]{referenceSort, ctx.mkSetSort(referenceSort), ctx.mkArraySort(referenceSort, nodeSort)},
                ctx.mkSetSort(referenceSort));

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr set = (ArrayExpr) ctx.mkBound(1, ctx.mkSetSort(referenceSort));
        Expr referenceMap = ctx.mkBound(2, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = getNextRef(node);

        set = ctx.mkSetAdd(set, ref);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);

        Expr recursiveCase = toSetFunc.apply(
                nextRef,
                ctx.mkSetAdd(set, ref),
                referenceMap);

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                set,
                recursiveCase);

        ctx.AddRecDef(toSetFunc, new Expr[]{ref, set, referenceMap}, body);
    }

    private void defineContainsAllFunc() {
        this.containsAllFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListContainsAllFunc"),
                new Sort[]{referenceSort, ctx.mkSetSort(referenceSort), ctx.mkArraySort(referenceSort, nodeSort)},
                ctx.mkBoolSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr set = (ArrayExpr) ctx.mkBound(1, ctx.mkSetSort(referenceSort));
        Expr referenceMap = ctx.mkBound(2, ctx.mkArraySort(referenceSort, nodeSort));

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);
        BoolExpr contains = ctx.mkSetMembership(ref, set);

        Expr recursiveCase = containsAllFunc.apply(
                nextRef,
                set,
                referenceMap);

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                ctx.mkTrue(),
                ctx.mkITE(contains, recursiveCase, ctx.mkFalse()));


        ctx.AddRecDef(containsAllFunc, new Expr[]{ref, set, referenceMap}, body);
    }

    private void defineEqualsFunc() {
        ArraySort arraySort = ctx.mkArraySort(referenceSort, nodeSort);

        this.equalsFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListEqualsFunc"),
                new Sort[]{referenceSort, arraySort, referenceSort, arraySort},
                ctx.mkBoolSort());

        Expr ref1 = ctx.mkBound(0, referenceSort);
        Expr referenceMap1 = ctx.mkBound(1, arraySort);
        Expr ref2 = ctx.mkBound(2, referenceSort);
        Expr referenceMap2 = ctx.mkBound(3, arraySort);

        Expr node1 = ctx.mkSelect(referenceMap1, ref1);
        Expr node2 = ctx.mkSelect(referenceMap2, ref2);

        Expr nextRef1 = getNextRef(node1);
        Expr nextRef2 = getNextRef(node2);

        BoolExpr hasReachedTheEnd1 = ctx.mkEq(ref1, nextRef1);
        BoolExpr hasReachedTheEnd2 = ctx.mkEq(ref2, nextRef2);
        BoolExpr sizesMatch = ctx.mkNot(ctx.mkXor(hasReachedTheEnd1, hasReachedTheEnd2));
        BoolExpr shouldContinue = ctx.mkAnd(
                ctx.mkNot(hasReachedTheEnd1),
                ctx.mkNot(hasReachedTheEnd2));

        BoolExpr equals = ctx.mkEq(getValue(node1), getValue(node2));

        Expr recursiveCase = equalsFunc.apply(
                nextRef1,
                referenceMap1,
                nextRef2,
                referenceMap2);

        Expr body = ctx.mkITE(
                shouldContinue,
                ctx.mkITE(equals, recursiveCase, ctx.mkFalse()),
                sizesMatch);

        ctx.AddRecDef(equalsFunc, new Expr[]{ref1, referenceMap1, ref2, referenceMap2}, body);
    }

    private ArrayExpr mkArray() {
        ArraySort arraySort = ctx.mkArraySort(referenceSort, nodeSort);
        return (ArrayExpr) ctx.mkFreshConst("Array", arraySort);
    }

    private ArrayExpr mkEmptyArray() {
        return ctx.mkConstArray(referenceSort, mkNullNode());
    }

    private Expr mkNullReference() {
        return ctx.mkConst("null", referenceSort);
    }

    private Expr mkNullNode() {
        Expr wrappedNull = sortUnion.wrapValue(ctx.mkNull());
        Expr nullReference = mkNullReference();
        return this.nodeSort.mkDecl().apply(
                wrappedNull, nullReference, nullReference, nullReference);
    }

    private Expr mkNextRef(LinkedListModel model) {
        IntExpr refCounter = model.getRefCounter();
        Expr ref = refCounter;
        refCounter = (IntExpr) ctx.mkAdd(refCounter, ctx.mkInt(1));
        model.setRefCounter(refCounter);
        return ref;
    }
}
