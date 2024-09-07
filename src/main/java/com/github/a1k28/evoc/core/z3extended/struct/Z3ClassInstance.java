package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.Context;

public class Z3ClassInstance implements IStack {
    private final Context ctx;
    private final Z3CachingFactory sortState;
    private final Z3Stack<Integer, MapModel> stack;
    private final Z3ExtendedSolver solver;

    public Z3ClassInstance(Context context, Z3CachingFactory sortState, Z3ExtendedSolver solver) {
        this.ctx = context;
        this.sortState = sortState;
        this.solver = solver;
        this.stack = new Z3Stack<>();
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }
}
