package com.github.a1k28.evoc.core.z3extended;

import com.microsoft.z3.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Z3ExtendedSolver {
    private final Context ctx;
    private final Solver solver;

    public void push() {
        solver.push();
    }

    public void pop() {
        solver.pop();
    }

    public void add(Expr expr) {
        solver.add(expr);
    }

    public Status check() {
        return solver.check();
    }

    public BoolExpr[] getAssertions() {
        return solver.getAssertions();
    }

    public Model getModel() {
        return solver.getModel();
    }

    public int minimize(Expr x) {
        // Binary search for the minimum value of x
        int low = 0;
        int high = Integer.MAX_VALUE;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            solver.push();
            solver.add(ctx.mkLe(x, ctx.mkInt(mid)));

            if (solver.check() == Status.SATISFIABLE) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
            solver.pop();
        }
        return result;
    }
}
