package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.instance.Z3ClassInstance;
import com.github.a1k28.evoc.core.z3extended.instance.Z3ListInstance;
import com.github.a1k28.evoc.core.z3extended.instance.Z3MapInstance;
import com.github.a1k28.evoc.core.z3extended.instance.Z3MethodMockInstance;
import com.github.a1k28.evoc.core.z3extended.struct.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.microsoft.z3.*;
import lombok.Getter;

@Getter
public class Z3ExtendedContext extends Context implements IStack {
//    private final Z3CachingFactory sortState;
//    private final Z3SetCollection z3SetCollection;
//    private final Z3ListCollection z3ListCollection;
    private final Z3ExtendedSolver solver;
    private final Z3ClassInstance classInstance;
    private final Z3MethodMockInstance methodMockInstance;
    private final Z3MapInstance mapInstance;
    private final Z3ListInstance listInstance;

    public Z3ExtendedContext() {
        super();
        Z3CachingFactory sortState = new Z3CachingFactory(this);
        Z3SortUnion sortUnion = new Z3SortUnion(this);

        Solver slvr = this.mkSolver();
        this.solver = new Z3ExtendedSolver(this, slvr, sortUnion);

        this.classInstance = new Z3ClassInstance(this);
        this.methodMockInstance = new Z3MethodMockInstance(this, solver);
        this.mapInstance = new Z3MapInstance(this, solver, sortState, sortUnion);
        this.listInstance = new Z3ListInstance(this, solver, sortState, sortUnion);
    }

    @Override
    public void push() {
//        this.z3SetCollection.push();
//        this.z3ListCollection.push();
        this.classInstance.push();
        this.methodMockInstance.push();
        this.mapInstance.push();
        this.listInstance.push();
    }

    @Override
    public void pop() {
//        this.z3SetCollection.pop();
//        this.z3ListCollection.pop();
        this.classInstance.pop();
        this.methodMockInstance.pop();
        this.mapInstance.pop();
        this.listInstance.pop();
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
}
