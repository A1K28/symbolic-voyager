package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.MethodMockExprModel;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Z3MethodMockInstance implements IStack {
    private final Context ctx;
    private final Z3ExtendedSolver solver;
    private final Z3Stack<String, MethodMockExprModel> stack;

    public Z3MethodMockInstance(Context ctx,
                                Z3ExtendedSolver solver) {
        this.ctx = ctx;
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

    public MethodMockExprModel constructor(
            Method method, List<Expr> args, ClassType throwType, Expr retVal)
            throws ClassNotFoundException {
        Expr referenceExpr = getInput(method, args);
        String refExprStr = evalStr(referenceExpr);
        Optional<MethodMockExprModel> optional = stack.get(refExprStr);
        if (optional.isPresent()) return optional.get();

        MethodMockExprModel model = new MethodMockExprModel(
                referenceExpr,
                method,
                args,
                throwType,
                retVal);
        stack.add(evalStr(referenceExpr), model);
        return model;
    }

    public MethodMockExprModel get(Expr referenceExpr) {
        return stack.get(evalStr(referenceExpr)).orElse(null);
    }

    // if parameters are equal, make sure the method mocks the same return value
    private Expr getInput(Method method, List<Expr> args) {
        Expr referenceExpr = ctx.mkString(UUID.randomUUID().toString());
//        Expr referenceExpr = ctx.mkFreshConst("mockReference", SortType.METHOD_MOCK.value(ctx));
        for (MethodMockExprModel model : stack.getAll()) {
            if (!method.equals(model.getMethod())) continue;
            if (model.getArgs().size() != args.size()) continue;
            BoolExpr eq = ctx.mkTrue();
            for (int i = 0; i < args.size(); i++) {
                Expr e1 = args.get(i);
                Expr e2 = model.getArgs().get(i);
                eq = ctx.mkAnd(eq, ctx.mkEq(e1, e2));
            }
            referenceExpr = ctx.mkITE(eq, model.getReferenceExpr(), referenceExpr);
        }
        return referenceExpr;
    }

    private String evalStr(Expr referenceExpr) {
        solver.check(); // obligatory check
        Expr refExprEvaluated = solver.getModel().eval(referenceExpr, true);
        return parseStr(refExprEvaluated);
    }

    private String parseStr(Expr expr) {
        String res = expr.toString();
        if (res.startsWith("\"") && res.endsWith("\""))
            res = res.substring(1, res.length()-1);
        return res;
    }
}
