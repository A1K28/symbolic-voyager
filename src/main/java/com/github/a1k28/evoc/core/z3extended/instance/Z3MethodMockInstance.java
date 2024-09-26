package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.github.a1k28.evoc.core.z3extended.model.MethodMockExprModel;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class Z3MethodMockInstance extends Z3AbstractHybridInstance implements IStack {
    private final Z3SortUnion sortUnion;
    private final Z3Stack<String, MethodMockExprModel> stack;

    public Z3MethodMockInstance(Z3ExtendedContext ctx,
                                Z3ExtendedSolver solver,
                                Z3SortUnion sortUnion) {
        super(ctx, solver, "MethodMockInstance",
                ctx.mkArraySort(ctx.mkIntSort(), sortUnion.getGenericSort()));
        this.sortUnion = sortUnion;
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
        Expr ref = retVal == null ? getInput(method, args) : retVal;
        Expr wrappedRef = wrapValue(ref);
        Optional<String> reference = evalReferenceStrict(wrappedRef);
        if (reference.isPresent()) return stack.get(reference.get()).orElseThrow();

        createMapping(wrappedRef);

        MethodMockExprModel model = new MethodMockExprModel(
                ref,
                method,
                args,
                throwType,
                retVal);
        stack.add(evalReference(wrappedRef), model);
        return model;
    }

    public MethodMockExprModel setExceptionType(Expr reference, ClassType exceptionType) {
        String ref = evalReference(wrapValue(reference));
        MethodMockExprModel model = stack.get(ref).orElseThrow();
        model.setExceptionType(exceptionType);
        stack.add(ref, model);
        return model;
    }

    public MethodMockExprModel get(Expr reference) {
        return stack.get(evalReference(wrapValue(reference))).orElse(null);
    }

    private Expr wrapValue(Expr reference) {
        if (reference instanceof ArrayExpr<?,?>)
            return reference;
        ArrayExpr input = ctx.mkConstArray(ctx.mkIntSort(), sortUnion.wrapValue(ctx.mkNull()));
        input = ctx.mkStore(input, ctx.mkInt(0), sortUnion.wrapValue(reference));
        return input;
    }

    // if parameters are equal, make sure the method mocks the same return value
    private Expr getInput(Method method, List<Expr> args) {
        ArrayExpr input = ctx.mkConstArray(ctx.mkIntSort(), sortUnion.wrapValue(ctx.mkNull()));
        Expr methodName = ctx.mkString(method.getName());
        input = ctx.mkStore(input, ctx.mkInt(0), sortUnion.wrapValue(methodName));
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                input = ctx.mkStore(input, ctx.mkInt(i+1), sortUnion.wrapValue(args.get(i)));
            }
        }
        return input;
    }
}
