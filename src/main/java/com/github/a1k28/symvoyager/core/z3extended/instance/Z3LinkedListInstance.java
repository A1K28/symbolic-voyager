package com.github.a1k28.symvoyager.core.z3extended.instance;

import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.github.a1k28.symvoyager.core.z3extended.model.LinkedListModel;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import com.github.a1k28.symvoyager.core.z3extended.sort.ArrayAndExprSort;
import com.github.a1k28.symvoyager.core.z3extended.sort.LinkedListNodeSort;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3Stack;
import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;

public class Z3LinkedListInstance extends Z3AbstractHybridInstance implements IStack {

    private final Z3SortUnion sortUnion;
    private final Z3Stack<String, LinkedListModel> stack;
    private final Sort valueSort;
    private final LinkedListNodeSort nodeSort;
    private final Sort referenceSort;
    private final ArrayAndExprSort arrayAndSizeHolderSort;
    private final ArraySort arraySort;

    private FuncDecl searchRecFunc;
    private FuncDecl findNodeByValueFunc;
    private FuncDecl findIdxByValueFunc;
    private FuncDecl findLastIdxByValueFunc;
    private FuncDecl<SetSort> toSetFunc;
    private FuncDecl<ArraySort> toArrayFunc;
    private FuncDecl<BoolSort> containsAllFunc;
    private FuncDecl<BoolSort> equalsFunc;
    private FuncDecl removeAllFunc;
    private FuncDecl retainAllFunc;
    private FuncDecl addAllFunc;
    private FuncDecl fillCapacityFunc;

    public Z3LinkedListInstance(Z3ExtendedContext context,
                                Z3ExtendedSolver solver,
                                Z3CachingFactory sortState,
                                Z3SortUnion sortUnion) {
        super(context, solver, "LinkedListInstance", SortType.ARRAY.value(context));
        this.sortUnion = sortUnion;
        this.stack = new Z3Stack<>();

        this.valueSort = sortUnion.getGenericSort();
        this.referenceSort = ctx.mkIntSort();
        this.nodeSort = sortState.mkLinkedListNodeSort(referenceSort, valueSort);
        this.arraySort = ctx.mkArraySort(referenceSort, nodeSort.getSort());
        this.arrayAndSizeHolderSort = sortState.mkArrayAndExprHolder(arraySort, ctx.mkIntSort());

        defineSearchFunc();
        defineFindNodeByValueFunc();
        defineFindIdxByValueFunc();
        defineFindLastIdxByValueFunc();
        defineToSetFunc();
        defineToArrayFunc();
        defineContainsAllFunc();
        defineEqualsFunc();
        defineRemoveAllFunc();
        defineRetainAllFunc();
        defineAddAllFunc();
        defineFillCapacityFunc();
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
        return constructor(var1, null, null, null, false).getReference();
    }

    public Expr constructor(Expr var1, IntExpr capacity) {
        return constructor(var1, null, null, capacity, false).getReference();
    }

    public Expr constructor(Expr var1, Expr var2) {
        return constructor(var1, var2, null, null, false).getReference();
    }

    public Expr constructorFrom(Expr var1) {
        Expr reference = ctx.mkFreshConst("reference", SortType.ARRAY.value(ctx));
        return constructor(reference, var1, null, null, false).getReference();
    }

    public Expr constructorOf(List<Expr> vars) {
        Expr[] args = null;
        if (vars.size() > 0)
            args = vars.toArray(new Expr[0]);

        Expr reference = ctx.mkFreshConst("reference", SortType.ARRAY.value(ctx));
        return constructor(reference, null, args, null, false).getReference();
    }

    public Expr parameterConstructor(Expr var1) {
        return constructor(var1, null, null, null, true).getReference();
    }

    private LinkedListModel constructor(
            Expr reference, Expr collection, Expr[] args, IntExpr capacity, boolean isSizeUnknown) {
        String ref = createMapping(reference);

        IntExpr size;
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

            head = nodeSort.mkDecl(nill, headRef, firstRef, headRef);
            tail = nodeSort.mkDecl(nill, tailRef, tailRef, lastRef);
        } else {
            referenceMap = mkEmptyArray();
            size = ctx.mkInt(0);

            head = nodeSort.mkDecl(nill, headRef, tailRef, headRef);
            tail = nodeSort.mkDecl(nill, tailRef, tailRef, headRef);
        }

        referenceMap = ctx.mkStore(referenceMap, headRef, head);
        referenceMap = ctx.mkStore(referenceMap, tailRef, tail);

        LinkedListModel linkedListModel = new LinkedListModel(
                reference, referenceMap, headRef, tailRef, size, refCounter);
        stack.add(ref, linkedListModel);

        if (collection != null) {
            addAll(reference, collection);
        } else if (args != null) {
            for (Expr arg : args) {
                add(reference, arg);
            }
        } else if (capacity != null) {
            fillByCapacity(linkedListModel, head, capacity);
        }

        return linkedListModel;
    }

    public Optional<LinkedListModel> getInitial(Expr var1) {
        return stack.getFirst(evalReference(var1));
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

    public BoolExpr addAll(Expr var1, Expr var2) {
        LinkedListModel target = copyModel(getModel(var1));
        LinkedListModel source = getModel(var2);

        Expr tailRef = target.getTailReference();
        return addAll(target, source, tailRef);
    }

    public BoolExpr addAll(Expr var1, IntExpr index, Expr var2) {
        LinkedListModel target = copyModel(getModel(var1));
        LinkedListModel source = getModel(var2);

        Expr lastRef = getReferenceByIndex(target.getReferenceMap(), target.getHeadReference(), index);
        return addAll(target, source, lastRef);
    }

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
        Expr nextRef = nodeSort.getNextRef(head);
        Expr node = findNodeByValueFunc.apply(
                nextRef, sortUnion.wrapValue(element), referenceMap);
        Expr ref = nodeSort.getRef(node);
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

    public BoolExpr removeAll(Expr var1, Expr var2) {
        LinkedListModel model1 = copyModel(getModel(var1));
        LinkedListModel model2 = getModel(var2);

        IntExpr size = model1.getSize();

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        Expr<SetSort> set = toSetFunc.apply(
                nodeSort.getNextRef(head2), ctx.mkEmptySet(valueSort), model2.getReferenceMap());
        Expr holder = removeAllFunc.apply(
                nodeSort.getNextRef(head1), model1.getReferenceMap(), set, ctx.mkInt(0));

        model1.setReferenceMap(arrayAndSizeHolderSort.getArray(holder));
        model1.setSize((IntExpr) arrayAndSizeHolderSort.getExpr(holder));

        return ctx.mkLt(model1.getSize(), size);
    }

    public BoolExpr retainAll(Expr var1, Expr var2) {
        LinkedListModel model1 = copyModel(getModel(var1));
        LinkedListModel model2 = getModel(var2);

        IntExpr size = model1.getSize();

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        Expr<SetSort> set = toSetFunc.apply(
                nodeSort.getNextRef(head2), ctx.mkEmptySet(valueSort), model2.getReferenceMap());
        Expr holder = retainAllFunc.apply(
                nodeSort.getNextRef(head1), model1.getReferenceMap(), set, ctx.mkInt(0));

        model1.setReferenceMap(arrayAndSizeHolderSort.getArray(holder));
        model1.setSize((IntExpr) arrayAndSizeHolderSort.getExpr(holder));

        return ctx.mkLt(model1.getSize(), size);
    }

    public BoolExpr contains(Expr var1, Expr element) {
        return ctx.mkNot(ctx.mkEq(indexOf(var1, element), ctx.mkInt(-1)));
    }

    public BoolExpr containsAll(Expr var1, Expr var2) {
        LinkedListModel model2 = getModel(var1);
        LinkedListModel model1 = getModel(var2);

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        Expr<SetSort> set = toSetFunc.apply(
                nodeSort.getNextRef(head2), ctx.mkEmptySet(valueSort), model2.getReferenceMap());
        return (BoolExpr) containsAllFunc.apply(nodeSort.getNextRef(head1), set, model1.getReferenceMap());
    }

    public Expr clear(Expr var1) {
        LinkedListModel model = copyModel(getModel(var1));

        Expr nill = sortUnion.wrapValue(ctx.mkNull());
        Expr headRef = model.getHeadReference();
        Expr tailRef = model.getTailReference();

        ArrayExpr referenceMap = mkEmptyArray();
        IntExpr size = ctx.mkInt(0);

        Expr head = nodeSort.mkDecl(nill, headRef, tailRef, headRef);
        Expr tail = nodeSort.mkDecl(nill, tailRef, tailRef, headRef);

        referenceMap = ctx.mkStore(referenceMap, headRef, head);
        referenceMap = ctx.mkStore(referenceMap, tailRef, tail);

        model.setReferenceMap(referenceMap);
        model.setSize(size);
//        model.setSizeUnknown(false);
        return null;
    }

    public Expr get(Expr var1, IntExpr index) {
        LinkedListModel model = getModel(var1);
        Expr ref = getReferenceByIndex(model.getReferenceMap(), model.getHeadReference(), index);
        Expr retrieved = ctx.mkSelect(model.getReferenceMap(), ref);
        Expr res = sortUnion.unwrapValue(nodeSort.getValue(retrieved), ctx.mkNull());
        return res;
    }

    public Expr hashCode(Expr var1) {
        LinkedListModel model = getModel(var1);

        Expr head = ctx.mkSelect(model.getReferenceMap(), model.getHeadReference());
        ArrayExpr array = ctx.mkConstArray(ctx.mkIntSort(), mkNullValue());

        array = (ArrayExpr) toArrayFunc.apply(
                nodeSort.getNextRef(head),
                model.getReferenceMap(),
                array,
                ctx.mkInt(0)
                );

        return array;
    }

//    public Expr subList(Expr var1, IntExpr fromIndex, IntExpr toIndex) {
//    }

    public Expr indexOf(Expr var1, Expr element) {
        LinkedListModel model = getModel(var1);
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr head = ctx.mkSelect(referenceMap, model.getHeadReference());
        Expr nextRef = nodeSort.getNextRef(head);
        return findIdxByValueFunc.apply(
                nextRef, sortUnion.wrapValue(element), ctx.mkInt(0), referenceMap);
    }

    public Expr lastIndexOf(Expr var1, Expr element) {
        LinkedListModel model = getModel(var1);
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr tail = ctx.mkSelect(referenceMap, model.getTailReference());
        Expr prevRef = nodeSort.getPrevRef(tail);
        IntExpr lastIdx = (IntExpr) findLastIdxByValueFunc.apply(
                prevRef, sortUnion.wrapValue(element), ctx.mkInt(0), referenceMap);
        return ctx.mkSub(model.getSize(), ctx.mkAdd(lastIdx, ctx.mkInt(1)));
    }

    public BoolExpr equals(Expr var1, Expr var2) {
        LinkedListModel model1 = getModel(var1);
        LinkedListModel model2 = getModel(var2);

        Expr head1 = ctx.mkSelect(model1.getReferenceMap(), model1.getHeadReference());
        Expr head2 = ctx.mkSelect(model2.getReferenceMap(), model2.getHeadReference());

        return (BoolExpr) equalsFunc.apply(nodeSort.getNextRef(head1), model1.getReferenceMap(),
                nodeSort.getNextRef(head2), model2.getReferenceMap());
    }

    public Expr set(Expr var1, IntExpr index, Expr element) {
        LinkedListModel model = getModel(var1);
        Expr wrappedElement = sortUnion.wrapValue(element);
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr ref = getReferenceByIndex(referenceMap, model.getHeadReference(), index);
        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr prevNodeUpdated = nodeSort.mkDecl(wrappedElement,
                ref, nodeSort.getNextRef(node), nodeSort.getPrevRef(node));
        // can only update the element if the ref was found
        BoolExpr canUpdate = ctx.mkAnd(
                ctx.mkNot(ctx.mkEq(model.getHeadReference(), ref)),
                ctx.mkNot(ctx.mkEq(model.getTailReference(), ref))
        );
        referenceMap = (ArrayExpr) ctx.mkITE(canUpdate,
                ctx.mkStore(referenceMap, ref, prevNodeUpdated),
                referenceMap);
        model.setReferenceMap(referenceMap);
        return sortUnion.unwrapValue(nodeSort.getValue(node), ctx.mkNull());
    }

    private Expr getReferenceByIndex(ArrayExpr referenceMap, Expr headRef, IntExpr index) {
        Expr head = ctx.mkSelect(referenceMap, headRef);
        Expr nextRef = nodeSort.getNextRef(head);
        return searchRecFunc.apply(nextRef, ctx.mkInt(0), index, referenceMap);
    }

    private BoolExpr addAll(LinkedListModel target, LinkedListModel source, Expr lastSourceRef) {
        IntExpr initialSize = target.getSize();
        Expr head2 = ctx.mkSelect(source.getReferenceMap(), source.getHeadReference());

        IntExpr initialSourceSize = target.getSize();

        Expr holder = addAllFunc.apply(
                lastSourceRef,
                target.getReferenceMap(),
                nodeSort.getNextRef(head2),
                source.getReferenceMap(),
                initialSize,
                target.getRefCounter());

        target.setReferenceMap(arrayAndSizeHolderSort.getArray(holder));
        target.setSize((IntExpr) arrayAndSizeHolderSort.getExpr(holder));

        IntExpr sizeDiff = (IntExpr) ctx.mkSub(target.getSize(), initialSourceSize);
        target.setRefCounter((IntExpr) ctx.mkAdd(target.getRefCounter(), sizeDiff));

        return ctx.mkGt(target.getSize(), initialSize);
    }

    private Expr remove(LinkedListModel model, Expr reference) {
        // can only remove the element if the ref was found
        BoolExpr canRemove = ctx.mkAnd(
                ctx.mkNot(ctx.mkEq(model.getHeadReference(), reference)),
                ctx.mkNot(ctx.mkEq(model.getTailReference(), reference))
        );

        ArrayExpr referenceMap = model.getReferenceMap();
        Expr node = ctx.mkSelect(referenceMap, reference);
        referenceMap = remove(referenceMap, node, canRemove);
        IntExpr size = (IntExpr) ctx.mkITE(canRemove, ctx.mkSub(model.getSize(), ctx.mkInt(1)), model.getSize());

        model.setSize(size);
        model.setReferenceMap(referenceMap);
        return sortUnion.unwrapValue(nodeSort.getValue(node), ctx.mkNull());
    }

    private ArrayExpr remove(ArrayExpr referenceMap, Expr node, BoolExpr canRemove) {
        Expr prevRef = nodeSort.getPrevRef(node);
        Expr nextRef = nodeSort.getNextRef(node);

        Expr prevNode = ctx.mkSelect(referenceMap, prevRef);
        Expr nextNode = ctx.mkSelect(referenceMap, nextRef);

        Expr prevNodeUpdated = nodeSort.mkDecl(nodeSort.getValue(prevNode),
                prevRef, nextRef, nodeSort.getPrevRef(prevNode));
        Expr nextNodeUpdated = nodeSort.mkDecl(nodeSort.getValue(nextNode),
                nextRef, nodeSort.getNextRef(nextNode), prevRef);

        referenceMap = (ArrayExpr) ctx.mkITE(canRemove, ctx.mkStore(referenceMap, prevRef, prevNodeUpdated), referenceMap);
        referenceMap = (ArrayExpr) ctx.mkITE(canRemove, ctx.mkStore(referenceMap, nextRef, nextNodeUpdated), referenceMap);
        return referenceMap;
    }

    private BoolExpr addBeforeReference(LinkedListModel model, Expr nextRef, Expr element) {
        ArrayExpr referenceMap = model.getReferenceMap();
        Expr newRef = mkNextRef(model);

        BoolExpr shouldAdd = ctx.mkTrue();
        Expr wrappedElement = sortUnion.wrapValue(element);
        referenceMap = addBeforeReference(referenceMap, nextRef, wrappedElement, newRef, shouldAdd);
        IntExpr size = (IntExpr) ctx.mkITE(shouldAdd,
                ctx.mkAdd(model.getSize(), ctx.mkInt(1)), model.getSize());

        model.setReferenceMap(referenceMap);
        model.setSize(size);
        return ctx.mkTrue();
    }

    /**
     *
     * @param referenceMap the array model
     * @param nextRef the reference, which should be next to the new element
     * @param wrappedElement the wrapped value
     * @param newRef the new reference value
     * @param shouldAdd a boolean value indicating whether the operation should proceed or not
     * @return true if the list was modified by the result of this call
     */
    private ArrayExpr addBeforeReference(
            ArrayExpr referenceMap, Expr nextRef, Expr wrappedElement, Expr newRef, BoolExpr shouldAdd) {
        Expr lastNode = ctx.mkSelect(referenceMap, nextRef);
        Expr prevRef = nodeSort.getPrevRef(lastNode);

        ArrayExpr originalReferenceMap = referenceMap;

        Expr node = nodeSort.mkDecl(wrappedElement, newRef, nextRef, prevRef);
        referenceMap = ctx.mkStore(referenceMap, newRef, node);

        Expr prevNode = ctx.mkSelect(referenceMap, prevRef);
        Expr prevNodeUpdated = nodeSort.mkDecl(nodeSort.getValue(prevNode), prevRef, newRef, nodeSort.getPrevRef(prevNode));
        referenceMap = ctx.mkStore(referenceMap, prevRef, prevNodeUpdated);

        Expr nextNodeUpdated = nodeSort.mkDecl(nodeSort.getValue(lastNode), nextRef, nodeSort.getNextRef(lastNode), newRef);
        referenceMap = ctx.mkStore(referenceMap, nextRef, nextNodeUpdated);

        return (ArrayExpr) ctx.mkITE(shouldAdd, referenceMap, originalReferenceMap);
    }

    private void fillByCapacity(LinkedListModel model, Expr head, IntExpr capacity) {
        ArrayExpr referenceMap = model.getReferenceMap();
        referenceMap = (ArrayExpr) fillCapacityFunc.apply(
                nodeSort.getNextRef(head),
                referenceMap,
                mkNullValue(),
                ctx.mkInt(2),
                ctx.mkAdd(capacity, ctx.mkInt(2)));
        model.setReferenceMap(referenceMap);
        model.setRefCounter((IntExpr) ctx.mkAdd(model.getRefCounter(), capacity));
    }

    private LinkedListModel getModel(Expr var1) {
        return getModel(var1, true);
    }

    private LinkedListModel getModel(Expr expr, boolean isSizeUnknown) {
        String reference = evalReference(expr);
        if (stack.get(reference).isEmpty())
            return createModel(expr, isSizeUnknown);
        return stack.get(reference).orElseThrow();
    }

    private LinkedListModel createModel(Expr expr, boolean isSizeUnknown) {
        return constructor(expr, null, null, null, isSizeUnknown);
    }

    private LinkedListModel copyModel(LinkedListModel model) {
        LinkedListModel newModel = new LinkedListModel(model);
        stack.add(evalReference(newModel.getReference()), newModel);
        return newModel;
    }

    // assuming index out of bounds does not occur!
    private void defineSearchFunc() {
        this.searchRecFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListSearchRecFunc"),
                new Sort[]{referenceSort, ctx.mkIntSort(), ctx.mkIntSort(), arraySort},
                referenceSort);

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr i = ctx.mkBound(1, ctx.mkIntSort());
        Expr index = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

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

    private void defineFindNodeByValueFunc() {
        this.findNodeByValueFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFindNodeByValueFunc"),
                new Sort[]{referenceSort, valueSort, arraySort},
                nodeSort.getSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr referenceMap = ctx.mkBound(2, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = nodeSort.getValue(node);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr isIllegalState = ctx.mkEq(ref, nextRef);
        BoolExpr condition = ctx.mkEq(value, retrievedVal);

        Expr recursiveCase = findNodeByValueFunc.apply(
                nextRef,
                value,
                referenceMap);

        Expr body = ctx.mkITE(
                ctx.mkOr(condition, isIllegalState),
                node,
                recursiveCase);

        ctx.AddRecDef(findNodeByValueFunc, new Expr[]{ref, value, referenceMap}, body);
    }

    private void defineFindIdxByValueFunc() {
        this.findIdxByValueFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFindIdxByValueFunc"),
                new Sort[]{referenceSort, valueSort, ctx.mkIntSort(), arraySort},
                ctx.mkIntSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr idx = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = nodeSort.getValue(node);
        Expr nextRef = nodeSort.getNextRef(node);

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
                new Sort[]{referenceSort, valueSort, ctx.mkIntSort(), arraySort},
                ctx.mkIntSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        Expr value = ctx.mkBound(1, valueSort);
        Expr idx = ctx.mkBound(2, ctx.mkIntSort());
        Expr referenceMap = ctx.mkBound(3, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr retrievedVal = nodeSort.getValue(node);
        Expr prevRef = nodeSort.getPrevRef(node);

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
                new Sort[]{referenceSort, ctx.mkSetSort(valueSort), arraySort},
                ctx.mkSetSort(valueSort));

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr set = (ArrayExpr) ctx.mkBound(1, ctx.mkSetSort(valueSort));
        Expr referenceMap = ctx.mkBound(2, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);

        Expr recursiveCase = toSetFunc.apply(
                nextRef,
                ctx.mkSetAdd(set, nodeSort.getValue(node)),
                referenceMap);

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                set,
                recursiveCase);

        ctx.AddRecDef(toSetFunc, new Expr[]{ref, set, referenceMap}, body);
    }

    private void defineToArrayFunc() {
        ArraySort indexedSort = ctx.mkArraySort(ctx.mkIntSort(), valueSort);
        this.toArrayFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListToArrayFunc"),
                new Sort[]{referenceSort, arraySort, indexedSort, ctx.mkIntSort()},
                indexedSort);

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr referenceMap = (ArrayExpr) ctx.mkBound(1, arraySort);
        ArrayExpr array = (ArrayExpr) ctx.mkBound(2, indexedSort);
        IntExpr idx = (IntExpr) ctx.mkBound(3, ctx.mkIntSort());

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);

        Expr recursiveCase = toArrayFunc.apply(
                nextRef,
                referenceMap,
                ctx.mkStore(array, idx, nodeSort.getValue(node)),
                ctx.mkAdd(idx, ctx.mkInt(1)));

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                array,
                recursiveCase);

        ctx.AddRecDef(toArrayFunc, new Expr[]{ref, referenceMap, array, idx}, body);
    }

    private void defineContainsAllFunc() {
        SetSort setSort = ctx.mkSetSort(valueSort);
        this.containsAllFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListContainsAllFunc"),
                new Sort[]{referenceSort, setSort, arraySort},
                ctx.mkBoolSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr set = (ArrayExpr) ctx.mkBound(1, setSort);
        Expr referenceMap = ctx.mkBound(2, arraySort);

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);
        BoolExpr contains = ctx.mkSetMembership(nodeSort.getValue(node), set);

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

        Expr nextRef1 = nodeSort.getNextRef(node1);
        Expr nextRef2 = nodeSort.getNextRef(node2);

        BoolExpr hasReachedTheEnd1 = ctx.mkEq(ref1, nextRef1);
        BoolExpr hasReachedTheEnd2 = ctx.mkEq(ref2, nextRef2);
        BoolExpr sizesMatch = ctx.mkNot(ctx.mkXor(hasReachedTheEnd1, hasReachedTheEnd2));
        BoolExpr shouldContinue = ctx.mkAnd(
                ctx.mkNot(hasReachedTheEnd1),
                ctx.mkNot(hasReachedTheEnd2));

        BoolExpr equals = ctx.mkEq(nodeSort.getValue(node1), nodeSort.getValue(node2));

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

    private void defineRemoveAllFunc() {
        SetSort setSort = ctx.mkSetSort(valueSort);
        this.removeAllFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListRemoveAllFunc"),
                new Sort[]{referenceSort, arraySort, setSort, ctx.mkIntSort()},
                arrayAndSizeHolderSort.getSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr referenceMap = (ArrayExpr) ctx.mkBound(1, arraySort);
        Expr set = ctx.mkBound(2, setSort);
        Expr idx = ctx.mkBound(3, ctx.mkIntSort());

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);
        BoolExpr contains = ctx.mkSetMembership(nodeSort.getValue(node), set);

        Expr recursiveCase = removeAllFunc.apply(
                nextRef,
                remove(referenceMap, node, contains),
                set,
                ctx.mkITE(contains, idx, ctx.mkAdd(idx, ctx.mkInt(1))));

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                arrayAndSizeHolderSort.mkDecl(referenceMap, idx),
                recursiveCase);

        ctx.AddRecDef(removeAllFunc, new Expr[]{ref, referenceMap, set, idx}, body);
    }

    private void defineRetainAllFunc() {
        SetSort setSort = ctx.mkSetSort(valueSort);
        this.retainAllFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListRetainAllFunc"),
                new Sort[]{referenceSort, arraySort, setSort, ctx.mkIntSort()},
                arrayAndSizeHolderSort.getSort());

        Expr ref = ctx.mkBound(0, referenceSort);
        ArrayExpr referenceMap = (ArrayExpr) ctx.mkBound(1, arraySort);
        Expr set = ctx.mkBound(2, setSort);
        Expr idx = ctx.mkBound(3, ctx.mkIntSort());

        Expr node = ctx.mkSelect(referenceMap, ref);
        Expr nextRef = nodeSort.getNextRef(node);

        BoolExpr hasReachedTheEnd = ctx.mkEq(ref, nextRef);
        BoolExpr contains = ctx.mkSetMembership(nodeSort.getValue(node), set);
        BoolExpr shouldRemove = ctx.mkNot(contains);

        Expr recursiveCase = retainAllFunc.apply(
                nextRef,
                remove(referenceMap, node, shouldRemove),
                set,
                ctx.mkITE(shouldRemove, idx, ctx.mkAdd(idx, ctx.mkInt(1))));

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                arrayAndSizeHolderSort.mkDecl(referenceMap, idx),
                recursiveCase);

        ctx.AddRecDef(retainAllFunc, new Expr[]{ref, referenceMap, set, idx}, body);
    }

    private void defineAddAllFunc() {
        this.addAllFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListAddAllFunc"),
                new Sort[]{referenceSort, arraySort,
                        referenceSort, arraySort,
                        ctx.mkIntSort(), ctx.mkIntSort()},
                arrayAndSizeHolderSort.getSort());

        Expr targetRef = ctx.mkBound(0, referenceSort);
        ArrayExpr targetRefMap = (ArrayExpr) ctx.mkBound(1, arraySort);
        Expr sourceRef = ctx.mkBound(2, referenceSort);
        ArrayExpr sourceRefMap = (ArrayExpr) ctx.mkBound(3, arraySort);
        IntExpr targetSize = (IntExpr) ctx.mkBound(4, ctx.mkIntSort());
        IntExpr refCounter = (IntExpr) ctx.mkBound(5, ctx.mkIntSort());

        Expr sourceNode = ctx.mkSelect(sourceRefMap, sourceRef);
        Expr nextRef = nodeSort.getNextRef(sourceNode);

        BoolExpr hasReachedTheEnd = ctx.mkEq(sourceRef, nextRef);
        BoolExpr shouldAdd = ctx.mkTrue();
        Expr wrappedElement = nodeSort.getValue(sourceNode);

        Expr recursiveCase = addAllFunc.apply(
                targetRef,
                addBeforeReference(targetRefMap, targetRef, wrappedElement, refCounter, shouldAdd),
                nextRef,
                sourceRefMap,
                ctx.mkITE(shouldAdd, ctx.mkAdd(targetSize, ctx.mkInt(1)), targetSize),
                ctx.mkAdd(refCounter, ctx.mkInt(1)));

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                arrayAndSizeHolderSort.mkDecl(targetRefMap, targetSize),
                recursiveCase);

        ctx.AddRecDef(addAllFunc, new Expr[]{
                targetRef,
                targetRefMap,
                sourceRef,
                sourceRefMap,
                targetSize,
                refCounter
        }, body);
    }

    private void defineFillCapacityFunc() {
        this.fillCapacityFunc = ctx.mkRecFuncDecl(
                ctx.mkSymbol("LinkedListFillCapacityFunc"),
                new Sort[]{referenceSort, arraySort, valueSort,
                        ctx.mkIntSort(), ctx.mkIntSort()},
                arraySort);

        Expr targetRef = ctx.mkBound(0, referenceSort);
        ArrayExpr targetRefMap = (ArrayExpr) ctx.mkBound(1, arraySort);
        Expr defaultValue = ctx.mkBound(2, valueSort);
        IntExpr idx = (IntExpr) ctx.mkBound(3, ctx.mkIntSort());
        IntExpr capacity = (IntExpr) ctx.mkBound(4, ctx.mkIntSort());

        BoolExpr hasReachedTheEnd = ctx.mkEq(idx, capacity);
        BoolExpr shouldAdd = ctx.mkTrue();

        Expr recursiveCase = fillCapacityFunc.apply(
                targetRef,
                addBeforeReference(targetRefMap, targetRef, defaultValue, idx, shouldAdd),
                defaultValue,
                ctx.mkAdd(idx, ctx.mkInt(1)),
                capacity);

        Expr body = ctx.mkITE(
                hasReachedTheEnd,
                targetRefMap,
                recursiveCase);

        ctx.AddRecDef(fillCapacityFunc, new Expr[]{
                targetRef,
                targetRefMap,
                defaultValue,
                idx,
                capacity
        }, body);
    }

    private ArrayExpr mkArray() {
        return (ArrayExpr) ctx.mkFreshConst("Array", arraySort);
    }

    private ArrayExpr mkEmptyArray() {
        return ctx.mkConstArray(referenceSort, mkNullNode());
    }

    private Expr mkNullReference() {
        return ctx.mkConst("null", referenceSort);
    }

    private Expr mkNullValue() {
        return sortUnion.wrapValue(ctx.mkNull());
    }

    private Expr mkNullNode() {
        Expr wrappedNull = mkNullValue();
        Expr nullReference = mkNullReference();
        return this.nodeSort.mkDecl(wrappedNull, nullReference, nullReference, nullReference);
    }

    private Expr mkNextRef(LinkedListModel model) {
        IntExpr refCounter = model.getRefCounter();
        Expr ref = refCounter;
        refCounter = (IntExpr) ctx.mkAdd(refCounter, ctx.mkInt(1));
        model.setRefCounter(refCounter);
        return ref;
    }
}
