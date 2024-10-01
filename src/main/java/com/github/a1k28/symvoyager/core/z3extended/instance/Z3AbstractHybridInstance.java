package com.github.a1k28.symvoyager.core.z3extended.instance;

import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.microsoft.z3.*;

import java.util.Optional;
import java.util.UUID;

/**
 * The name hybrid is due to the fact that
 * the class uses a combination of expressions
 * and solver evaluated variable to store
 * expression maps in memory.
 *
 * This might be the fastest way to achieve this.
 * (Although not sure about long term results. more tests are needed).
 */
public abstract class Z3AbstractHybridInstance {
    protected final Z3ExtendedContext ctx;
    protected final Z3ExtendedSolver solver;
    private final FuncDecl<SeqSort<?>> arrayReferenceMap;

    protected Z3AbstractHybridInstance(
            Z3ExtendedContext ctx, Z3ExtendedSolver solver, String name, Sort sort) {
        this.ctx = ctx;
        this.solver = solver;
        this.arrayReferenceMap = ctx.mkFreshFuncDecl(
                name+"ArrayReferenceMap", new Sort[]{sort}, ctx.mkStringSort());
    }

    protected String createMapping(Expr reference) {
        String ref = UUID.randomUUID().toString();
        Expr strRefExpr = ctx.mkString(ref);
        BoolExpr condition = ctx.mkEq(arrayReferenceMap.apply(reference), strRefExpr);
        solver.add(condition);
        return ref;
    }

    protected String evalReference(Expr reference) {
        solver.check(); // obligatory check
        Expr strRefExpr = arrayReferenceMap.apply(reference);
        Expr strRef = solver.getModel().eval(strRefExpr, true);
        return parseStr(strRef);
    }

    protected Optional<String> evalReferenceStrict(Expr reference) {
        Expr strRefExpr = arrayReferenceMap.apply(reference);
        if (!solver.isUnsatisfiable(ctx.mkEq(strRefExpr, ctx.mkString(""))))
            return Optional.empty();
        solver.check(); // obligatory check
        Expr strRef = solver.getModel().eval(strRefExpr, true);
        return Optional.of(parseStr(strRef));
    }

    private String parseStr(Expr expr) {
        String res = expr.toString();
        if (res.startsWith("\"") && res.endsWith("\""))
            res = res.substring(1, res.length()-1);
        return res;
    }
}
