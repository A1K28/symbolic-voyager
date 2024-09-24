package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.microsoft.z3.*;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public abstract class Z3AbstractHybridInstance {
    protected final Z3ExtendedContext ctx;
    protected final Z3ExtendedSolver solver;

    private FuncDecl<SeqSort> arrayReferenceMap;

    protected void defineMapFunc(String name, Sort sort) {
        this.arrayReferenceMap = ctx
                .mkFuncDecl(name, sort, ctx.mkStringSort());
    }

    protected void createMapping(Expr reference) {
        Expr strRefExpr = ctx.mkString(UUID.randomUUID().toString());
        BoolExpr condition = ctx.mkEq(arrayReferenceMap.apply(reference), strRefExpr);
        solver.add(condition);
    }

    protected String evalReference(Expr reference) {
        System.out.println(reference);
        solver.check(); // obligatory check
        Expr strRefExpr = arrayReferenceMap.apply(reference);
        Expr strRef = solver.getModel().eval(strRefExpr, true);
        return parseStr(strRef);
    }

    protected Optional<String> evalReferenceStrict(Expr reference) {
        System.out.println(reference);
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
        System.out.println(res);
        return res;
    }
}
