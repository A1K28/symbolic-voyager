package com.github.a1k28.symvoyager.core.z3extended.instance;

import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.github.a1k28.symvoyager.core.z3extended.model.IteratorInstanceModel;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3Stack;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public class Z3IteratorInstance extends Z3AbstractHybridInstance implements IStack {
    private final Z3Stack<String, IteratorInstanceModel> stack;
    private final Z3SortUnion sortUnion;

    public Z3IteratorInstance(Z3ExtendedContext ctx,
                              Z3ExtendedSolver solver,
                              Z3SortUnion sortUnion) {
        super(ctx, solver, "IteratorInstance", sortUnion.getGenericSort());
        this.stack = new Z3Stack<>();
        this.sortUnion = sortUnion;
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public Expr listConstructor(Expr base) {
        Expr reference = ctx.mkFreshConst("reference", SortType.ITERATOR.value(ctx));
        return constructor(reference, base, IteratorInstanceModel.Type.LIST).getReference();
    }

    private IteratorInstanceModel constructor(Expr reference,
                                              Expr base,
                                              IteratorInstanceModel.Type type) {
        String ref = createMapping(sortUnion.wrapValue(reference));
        IteratorInstanceModel model = new IteratorInstanceModel();
        model.setReference(reference);
        model.setBase(base);
        model.setType(type);

        if (type == IteratorInstanceModel.Type.LIST) {
            model.setIndex(ctx.mkInt(0));
            model.setSize(ctx.getLinkedListInstance().size(base));
        } else if (type == IteratorInstanceModel.Type.UNKNOWN) {
            model.setIndex((IntExpr) ctx.mkFreshConst("index", ctx.mkIntSort()));
            model.setSize((IntExpr) ctx.mkFreshConst("size", ctx.mkIntSort()));
        }

        stack.add(ref, model);

        return model;
    }

    public BoolExpr hasNext(Expr var1) {
        IteratorInstanceModel model = getModel(var1);
        return ctx.mkLt(model.getIndex(), model.getSize());
    }

    // assuming next element exists
    public Expr next(Expr var1) {
        IteratorInstanceModel model = getModel(var1);
        if (model.getType() == IteratorInstanceModel.Type.LIST) {
            Expr expr = ctx.getLinkedListInstance().get(model.getBase(), model.getIndex());
            model.setIndex((IntExpr) ctx.mkAdd(model.getIndex(), ctx.mkInt(1)));
            return expr;
        }
        if (model.getType() == IteratorInstanceModel.Type.UNKNOWN) {
            return ctx.mkFreshConst("element", sortUnion.getGenericSort());
        }
        throw new IllegalStateException("Unsupported iterator instance type: " + model.getType());
    }

    private IteratorInstanceModel getModel(Expr expr) {
        String reference = evalReference(sortUnion.wrapValue(expr));
        if (stack.get(reference).isEmpty())
            return createModel(expr);
        return stack.get(reference).orElseThrow();
    }

    public IteratorInstanceModel createModel(Expr base) {
        Expr reference = ctx.mkFreshConst("reference", SortType.ITERATOR.value(ctx));
        return constructor(reference, base, IteratorInstanceModel.Type.UNKNOWN);
    }
}
