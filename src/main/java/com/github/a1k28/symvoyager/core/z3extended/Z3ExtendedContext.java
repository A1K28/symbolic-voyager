package com.github.a1k28.symvoyager.core.z3extended;

import com.github.a1k28.symvoyager.core.z3extended.instance.*;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3SortUnion;
import com.microsoft.z3.*;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Getter
public class Z3ExtendedContext extends Context implements IStack {
    private final Z3ExtendedSolver solver;
    private final Z3SortUnion sortUnion;

    private final List<IStack> stacks;
    private final Z3ClassInstance classInstance;
    private final Z3MethodMockInstance methodMockInstance;
    private final Z3MapInstance mapInstance;
    private final Z3LinkedListInstance linkedListInstance;
    private final Z3IteratorInstance iteratorInstance;

    public Z3ExtendedContext() {
        super();
        Z3CachingFactory sortState = new Z3CachingFactory(this);
        this.sortUnion = new Z3SortUnion(this);

        this.solver = new Z3ExtendedSolver(this, this.mkSolver());

        this.classInstance = new Z3ClassInstance(this, solver, sortUnion);
        this.methodMockInstance = new Z3MethodMockInstance(this, solver, sortUnion);
        this.mapInstance = new Z3MapInstance(this, solver, sortState, sortUnion);
        this.linkedListInstance = new Z3LinkedListInstance(this, solver, sortState, sortUnion);
        this.iteratorInstance = new Z3IteratorInstance(this, solver, sortUnion);

        this.stacks = List.of(
                classInstance,
                methodMockInstance,
                mapInstance,
                linkedListInstance,
                iteratorInstance);
    }

    @Override
    public void push() {
        stacks.forEach(IStack::push);
    }

    @Override
    public void pop() {
        stacks.forEach(IStack::pop);
    }

    public BoolExpr mkEq(Expr<?> var1, Expr<?> var2) {
        boolean e1IsNull = SortType.NULL.equals(var1.getSort());
        boolean e2IsNull = SortType.NULL.equals(var2.getSort());
        if (e1IsNull && e2IsNull) return this.mkBool(true);
        if (e1IsNull ^ e2IsNull) return this.mkBool(false);

        boolean isVar1Generic = sortUnion.getGenericSort().equals(var1.getSort());
        boolean isVar2Generic = sortUnion.getGenericSort().equals(var2.getSort());
        if (isVar1Generic && !isVar2Generic)
            var2 = sortUnion.wrapValue(var2);
        if (!isVar1Generic && isVar2Generic)
            var1 = sortUnion.wrapValue(var1);

        return super.mkEq(var1, var2);
    }

    // strings
    public <R extends Sort> SeqExpr<R> mkStringConcatString(Expr expr1, Expr expr2) {
        Expr<SeqSort<R>> e1 = (Expr<SeqSort<R>>) expr1;
        Expr<SeqSort<R>> e2 = (Expr<SeqSort<R>>) expr2;
        return this.mkConcat(e1, e2);
    }

    public <R extends Sort> SeqExpr<R> mkStringConcatInt(Expr expr1, Expr expr2) {
        Expr<SeqSort<R>> e1 = expr1.getSort().getClass() == IntSort.class ?
                this.intToString(expr1) : (Expr<SeqSort<R>>) expr1;
        Expr<SeqSort<R>> e2 = expr2.getSort().getClass() == IntSort.class ?
                this.intToString(expr2) : (Expr<SeqSort<R>>) expr2;
        return this.mkConcat(e1, e2);
    }

    public IntExpr mkStringCompare(Expr expr1, Expr expr2) {
        Expr<SeqSort<CharSort>> seqExpr1 = expr1;
        Expr<SeqSort<CharSort>> seqExpr2 = expr2;

        BoolExpr isEq = this.mkEq(expr1, expr2);
        BoolExpr isLt;

        try {
            Method checkCtxMatchMethod = this.getClass().getSuperclass().getDeclaredMethod(
                    "checkContextMatch", Z3Object.class, Z3Object.class);
            checkCtxMatchMethod.setAccessible(true);

            Method nCtxMethod = this.getClass().getMethod("nCtx");
            nCtxMethod.setAccessible(true);

            Method getNativeObjMethod = Z3Object.class.getDeclaredMethod(
                    "getNativeObject");
            getNativeObjMethod.setAccessible(true);

            Constructor boolExprConstructor = BoolExpr.class.getDeclaredConstructor(
                    this.getClass().getSuperclass(), long.class);
            boolExprConstructor.setAccessible(true);

            checkCtxMatchMethod.invoke(this, expr1, expr2);
            long res = Native.mkStrLt((Long) nCtxMethod.invoke(this),
                    (Long) getNativeObjMethod.invoke(expr1),
                    (Long) getNativeObjMethod.invoke(expr2));
            isLt = (BoolExpr) boolExprConstructor.newInstance(this, res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return (IntExpr) this.mkITE(
                isEq,
                this.mkInt(0),
                this.mkITE(isLt, this.mkInt(-1), this.mkInt(1)));
    }

    public Expr mkNull() {
        return this.mkConst("null", SortType.NULL.value(this));
    }

    public boolean containsAssertion(Expr assertion) {
        return Arrays.asList(solver.getAssertions()).contains(assertion);
    }
}
