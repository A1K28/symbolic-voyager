package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.ClassInstanceModel;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.struct.Z3ClassInstance;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Map;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;

public class Z3ExtendedContext extends Context implements IStack {
    private final Z3CachingFactory sortState;
//    private final Z3SetCollection z3SetCollection;
//    private final Z3ListCollection z3ListCollection;
    private final Z3Map z3Map;
    private final Z3ClassInstance z3ClassInstance;
    private final Z3ExtendedSolver solver;

    public Z3ExtendedContext() {
        super();
        this.sortState = new Z3CachingFactory();
//        this.z3ListCollection = new Z3ListCollection(this);
//        this.z3SetCollection = new Z3SetCollection(this);

        Solver slvr = this.mkSolver();
        this.solver = new Z3ExtendedSolver(this, slvr);

        this.z3Map = new Z3Map(this, sortState, solver);
        this.z3ClassInstance = new Z3ClassInstance(this, sortState, solver);
    }

    @Override
    public void push() {
//        this.z3SetCollection.push();
//        this.z3ListCollection.push();
        this.z3Map.push();
        this.z3ClassInstance.push();
    }

    @Override
    public void pop() {
//        this.z3SetCollection.pop();
//        this.z3ListCollection.pop();
        this.z3Map.pop();
        this.z3ClassInstance.pop();
    }

    public Z3ExtendedSolver getSolver() {
        return this.solver;
    }

    // lists
//    public Expr mkList(Expr var1) {
//        return z3ListCollection.constructor(var1);
//    }
//
//    public Expr mkListWithCapacity(Expr var1, IntExpr capacity) {
//        return z3ListCollection.constructor(var1, parseInt(capacity));
//    }
//
//    public Expr mkListWithCollection(Expr var1, Expr var2) {
//        return z3ListCollection.constructor(var1, var2);
//    }
//
//    public Expr mkListWithCollection(List<Expr> vars) {
//        return z3ListCollection.constructor(getSort(vars), vars.toArray(new Expr[0]));
//    }
//
//    public Expr mkListLength(Expr var1) {
//        return z3ListCollection.size(var1);
//    }
//
//    public BoolExpr mkListIsEmpty(Expr var1) {
//        return z3ListCollection.isEmpty(var1);
//    }
//
//    public BoolExpr mkListAdd(Expr var1, Expr element) {
//        return z3ListCollection.add(var1, element);
//    }
//
//    public BoolExpr mkListAdd(Expr var1, IntExpr index, Expr var2) {
//        return z3ListCollection.add(var1, index, var2);
//    }
//
//    public BoolExpr mkListAddAll(Expr var1, Expr var2) {
//        return z3ListCollection.addAll(var1, var2);
//    }
//
//    public BoolExpr mkListAddAll(Expr var1, IntExpr index, Expr var2) {
//        return z3ListCollection.addAll(var1, index, var2);
//    }
//
//    public BoolExpr mkListRemove(Expr var1, Expr element) {
//        return z3ListCollection.remove(var1, element);
//    }
//
//    public Expr mkListRemove(Expr var1, IntExpr index) {
//        return z3ListCollection.remove(var1, index);
//    }
//
//    public BoolExpr mkListRemoveAll(Expr var1, Expr var2) {
//        return z3ListCollection.removeAll(var1, var2);
//    }
//
//    public BoolExpr mkListContains(Expr var1, Expr element) {
//        return z3ListCollection.contains(var1, element);
//    }
//
//    public BoolExpr mkListContainsAll(Expr var1, Expr var2) {
//        return z3ListCollection.containsAll(var1, var2);
//    }
//
//    public BoolExpr mkListRetainAll(Expr var1, Expr var2) {
//        return z3ListCollection.retainAll(var1, var2);
//    }
//
//    public Expr mkListClear(Expr var1) {
//        return z3ListCollection.clear(var1);
//    }
//
//    public BoolExpr mkListEquals(Expr var1, Expr var2) {
//        return z3ListCollection.equals(var1, var2);
//    }
//
//    public Expr mkListGet(Expr var1, IntExpr index) {
//        return z3ListCollection.get(var1, index);
//    }
//
//    public Expr mkListSet(Expr var1, IntExpr index, Expr element) {
//        return z3ListCollection.set(var1, index, element);
//    }
//
//    public Expr mkListHashCode(Expr var1) {
//        return z3ListCollection.hashCode(var1);
//    }
//
//    public Expr mkListSublist(Expr var1, IntExpr fromIndex, IntExpr toIndex) {
//        return z3ListCollection.subList(var1, fromIndex, toIndex);
//    }
//
//    public Expr mkListIndexOf(Expr var1, Expr element) {
//        return z3ListCollection.indexOf(var1, element);
//    }
//
//    public Expr mkListLastIndexOf(Expr var1, Expr element) {
//        return z3ListCollection.lastIndexOf(var1, element);
//    }

    // maps
    public Optional<MapModel> getInitialMap(Expr var1) {
        return z3Map.getInitialMap(var1);
    }

    public Expr mkMapInit(Expr var1) {
        return z3Map.constructor(var1);
    }

    public Expr mkMapInitFromMap(Expr var1, Expr var2) {
        return z3Map.constructor(var1, var2);
    }

    public Expr mkMapGet(Expr var1, Expr key) {
        return z3Map.get(var1, key);
    }

    public Expr mkMapPut(Expr var1, Expr key, Expr value) {
        return z3Map.put(var1, key, value);
    }

    public Expr mkMapLength(Expr var1) {
        return z3Map.size(var1);
    }

    public BoolExpr mkMapIsEmpty(Expr var1) {
        return z3Map.isEmpty(var1);
    }

    public BoolExpr mkMapContainsKey(Expr var1, Expr key) {
        return z3Map.containsKey(var1, key);
    }

    public BoolExpr mkMapContainsKey(MapModel model, Expr key) {
        return z3Map.containsKey(model, key);
    }

    public BoolExpr mkMapContainsKeyValuePair(MapModel model, Expr key, Expr value) {
        return z3Map.containsKeyValuePair(model, key, value);
    }

    public BoolExpr mkMapExistsByKeyAndValueCondition(MapModel model, Expr retrieved, Expr key, Expr value) {
        return z3Map.existsByKeyAndValueCondition(model, retrieved, key, value);
    }

    public BoolExpr mkMapContainsValue(Expr var1, Expr value) {
        return z3Map.containsValue(var1, value);
    }

    public Expr mkMapRemove(Expr var1, Expr key) {
        return z3Map.remove(var1, key);
    }

    public Expr mkMapPutAll(Expr var1, Expr var2) {
        return z3Map.putAll(var1, var2);
    }

    public Expr mkMapClear(Expr var1) {
        return z3Map.clear(var1);
    }

    public Expr mkMapEquals(Expr var1, Expr var2) {
        return z3Map.equals(var1, var2);
    }

    public Expr mkMapGetOrDefault(Expr var1, Expr key, Expr def) {
        return z3Map.getOrDefault(var1, key, def);
    }

    public Expr mkMapPutIfAbsent(Expr var1, Expr key, Expr value) {
        return z3Map.putIfAbsent(var1, key, value);
    }

    public Expr mkMapRemove(Expr var1, Expr key, Expr value) {
        return z3Map.removeByKeyAndValue(var1, key, value);
    }

    public Expr mkMapReplace(Expr var1, Expr key, Expr value) {
        return z3Map.replace(var1, key, value);
    }

    public Expr mkMapReplace(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        return z3Map.replaceByKeyAndValue(var1, key, oldValue, newValue);
    }

    public Expr mkMapCopyOf(Expr var1) {
        return z3Map.copyOf(var1);
    }

    public Expr mkMapOf(List<Expr> vars) {
        return z3Map.of(vars.toArray(new Expr[0]));
    }

    public ClassInstanceModel mkClassInstance(Class<?> clazz) throws ClassNotFoundException {
        return z3ClassInstance.constructor(clazz);
    }

    public ClassInstanceModel mkClassInstance(Expr expr, Class<?> clazz) throws ClassNotFoundException {
        return z3ClassInstance.constructor(expr, clazz);
    }

    public Expr mkClassInitialize(Expr expr) {
        return z3ClassInstance.initialize(expr);
    }

    public Optional<ClassInstanceModel> getClassInstance(Expr expr) {
        return z3ClassInstance.getClassInstance(expr);
    }


    // sets
//    @Override
//    public <D extends Sort> ArrayExpr<D, BoolSort> mkSetAdd(Expr<ArraySort<D, BoolSort>> var1, Expr<D> var2) {
//        z3SetCollection.add(System.identityHashCode(var1), var2);
//        return super.mkSetAdd(var1, var2);
//    }
//
//    public Expr mkSetLength(Expr expr) {
//        return super.mkInt(z3SetCollection.size(System.identityHashCode(expr)));
//    }
//
//    public Expr mkSetContains(Expr var1, Expr var2) {
//        return z3SetCollection.contains(System.identityHashCode(var1), var2);
//    }
//
//    public Expr mkSetRemove(Expr var1, Expr var2) {
//        z3SetCollection.remove(System.identityHashCode(var1), var2);
//        return super.mkSetDel(var1, var2);
//    }

    private int parseInt(IntExpr expr) {
        return Integer.parseInt(expr.toString());
    }

    private Sort getSort(List<Expr> exprs) {
        return sortState.getSort(this, exprs);
    }
}
