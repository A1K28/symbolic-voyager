package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.struct.Z3MapCollection;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;

public class Z3ExtendedContext extends Context implements IStack {
    private final Z3SortState sortState;
//    private final Z3SetCollection z3SetCollection;
//    private final Z3ListCollection z3ListCollection;
    private final Z3MapCollection z3MapCollection;

    public Z3ExtendedContext() {
        super();
        this.sortState = new Z3SortState();
//        this.z3ListCollection = new Z3ListCollection(this);
        this.z3MapCollection = new Z3MapCollection(this, sortState);
//        this.z3SetCollection = new Z3SetCollection(this);
    }

    @Override
    public void push() {
//        this.z3SetCollection.push();
//        this.z3ListCollection.push();
        this.z3MapCollection.push();
    }

    @Override
    public void pop() {
//        this.z3SetCollection.pop();
//        this.z3ListCollection.pop();
        this.z3MapCollection.pop();
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
    public Expr mkMapInit(Expr var1) {
        return z3MapCollection.constructor(var1);
    }

    public Optional<MapModel> getMap(Expr var1) {
        return z3MapCollection.getMap(var1);
    }

    public Optional<MapModel> getMapFirst(Expr var1) {
        return z3MapCollection.getMapFirst(var1);
    }

    public Expr mkMapGet(Expr var1, Expr key) {
        return z3MapCollection.get(var1, key);
    }

    public Expr mkMapGetEntry(Expr var1, Expr key) {
        return z3MapCollection.getEntry(var1, key);
    }

    public Expr mkMapPut(Expr var1, Expr key, Expr value) {
        return z3MapCollection.put(var1, key, value);
    }

    public Expr mkMapLength(Expr var1) {
        return z3MapCollection.size(var1);
    }

    public Expr mkMapUnresolvedLength(Expr var1) {
        return z3MapCollection.unresolvedSize(var1);
    }

    public BoolExpr mkMapIsEmpty(Expr var1) {
        return z3MapCollection.isEmpty(var1);
    }

    public BoolExpr mkMapContainsKey(Expr var1, Expr key) {
        return z3MapCollection.containsKey(var1, key);
    }

    public BoolExpr mkMapContainsValue(Expr var1, Expr value) {
        return z3MapCollection.containsValue(var1, value);
    }

    public Expr mkMapRemove(Expr var1, Expr key) {
        return z3MapCollection.remove(var1, key);
    }

    public Expr mkMapPutAll(Expr var1, Expr var2) {
        return z3MapCollection.putAll(var1, var2);
    }

    public Expr mkMapClear(Expr var1) {
        return z3MapCollection.clear(var1);
    }

    public Expr mkMapEquals(Expr var1, Expr var2) {
        return z3MapCollection.equals(var1, var2);
    }

    public Expr mkMapGetOrDefault(Expr var1, Expr key, Expr def) {
        return z3MapCollection.getOrDefault(var1, key, def);
    }

    public Expr mkMapPutIfAbsent(Expr var1, Expr key, Expr value) {
        return z3MapCollection.putIfAbsent(var1, key, value);
    }

    public Expr mkMapRemove(Expr var1, Expr key, Expr value) {
        return z3MapCollection.removeByKeyAndValue(var1, key, value);
    }

    public Expr mkMapReplace(Expr var1, Expr key, Expr value) {
        return z3MapCollection.replace(var1, key, value);
    }

    public Expr mkMapReplace(Expr var1, Expr key, Expr oldValue, Expr newValue) {
        return z3MapCollection.replaceByKeyAndValue(var1, key, oldValue, newValue);
    }

    public Expr mkMapCopyOf(Expr var1) {
        return z3MapCollection.copyOf(var1);
    }

    public Expr mkMapOf(List<Expr> vars) {
        return z3MapCollection.of(vars.toArray(new Expr[0]));
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
