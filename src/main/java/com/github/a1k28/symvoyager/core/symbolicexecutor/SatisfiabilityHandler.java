package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.*;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.*;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import com.github.a1k28.symvoyager.core.z3extended.instance.Z3MapInstance;
import com.github.a1k28.symvoyager.core.z3extended.model.*;
import com.github.a1k28.symvoyager.helper.Logger;
import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.microsoft.z3.*;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SatisfiabilityHandler {
    private static final Logger log = Logger.getInstance(SatisfiabilityHandler.class);

    private final Z3Translator z3t;
    private Z3ExtendedSolver solver;
    private Z3ExtendedContext ctx;

    public SatisfiabilityHandler(Z3Translator z3t) {
        this.z3t = z3t;
        refresh();
    }

    public void refresh() {
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
    }

    Z3Status checkSatisfiability(SMethodPath sMethodPath, SNode node, SType type) {
        if (solver.check() != Status.SATISFIABLE) {
            log.info("Path is unsatisfiable - " + sMethodPath.getMethod().getName() + "\n");
            return Z3Status.UNSATISFIABLE_END;
        }
        if (SType.INVOKE == type || SType.GOTO == type || SType.THROW == type) {
            return Z3Status.SATISFIABLE_END;
        }
        if (SType.THROW_END == type) {
            Class exceptionType = sMethodPath.getSymbolicVarStack()
                    .get(z3t.getValueName(((JThrowStmt) node.getUnit()).getOp())).get()
                    .getClassType();
            handleSatisfiability(sMethodPath, null, exceptionType);
            return Z3Status.SATISFIABLE_END;
        }
        if (SType.RETURN == type || SType.RETURN_VOID == type) {
            // if tail
            SVarEvaluated returnValue = getReturnValue(sMethodPath, node);
            handleSatisfiability(sMethodPath, returnValue, null);
            return Z3Status.SATISFIABLE_END;
        }
        return Z3Status.SATISFIABLE;
    }

    private void handleSatisfiability(
            SMethodPath sMethodPath,
            SVarEvaluated returnValue,
            Class<? extends Throwable> exceptionType) {
        log.info("Path is satisfiable - " + sMethodPath.getMethod().getName());

        List<SVarEvaluated> fieldsEvaluated = new ArrayList<>();
        List<SVarEvaluated> parametersEvaluated = new ArrayList<>();
        List<SMethodMockEvaluated> mockedMethodsEvaluated = new ArrayList<>();
        List<SVar> symbolicVars = sMethodPath.getAllSymbolicVars();
        for (SVar var : symbolicVars) {
            if (!var.isDeclaration()) continue;
            if (var.getType() != VarType.PARAMETER
                    && var.getType() != VarType.FIELD
                    && var.getType() != VarType.METHOD_MOCK) continue;

            if (var.getType() == VarType.FIELD) {
                Object evaluated = evaluateSatisfiableExpression(
                        sMethodPath, var, var.getExpr());
                SVarEvaluated sVarEvaluated = new SVarEvaluated(var, evaluated);
                fieldsEvaluated.add(sVarEvaluated);
            } else if (var.getType() == VarType.METHOD_MOCK) {
                SMethodMockEvaluated sVarEvaluated = handleMockExpression(sMethodPath, var);
                mockedMethodsEvaluated.add(sVarEvaluated);
            } else {
                // parameter
                Object evaluated = evaluateSatisfiableExpression(
                        sMethodPath, var, var.getExpr());
                SVarEvaluated sVarEvaluated = new SVarEvaluated(var, evaluated);
                parametersEvaluated.add(sVarEvaluated);
            }
        }

        if (returnValue != null)
            log.trace(returnValue.getEvaluated() + " - ret. val.");
        if (exceptionType != null)
            log.trace(exceptionType + " - exception type");

        SatisfiableResult satisfiableResult = new SatisfiableResult(
                solver.getAssertions(),
                fieldsEvaluated,
                parametersEvaluated,
                mockedMethodsEvaluated,
                returnValue,
                exceptionType
        );

        sMethodPath.getSatisfiableResults().getResults().add(satisfiableResult);
        log.empty();
    }

    private SVarEvaluated getReturnValue(SMethodPath methodPath, SNode node) {
        if (node.getType() == SType.RETURN) {
            JReturnStmt stmt = (JReturnStmt) node.getUnit();
            Expr expr = z3t.translateValue(
                    stmt.getOp(), VarType.RETURN_VALUE, methodPath);
            Class<?> classType = SootInterpreter.translateType(stmt.getOp().getType());
            SVar svar = new SVar(z3t.getValueName(stmt.getOp()),
                    expr,
                    VarType.RETURN_VALUE,
                    classType,
                    true);
            Object evaluated = evaluateSatisfiableExpression(methodPath, svar, expr);
            return new SVarEvaluated(svar, evaluated);
        }
        return null;
    }

    private SMethodMockEvaluated handleMockExpression(SMethodPath methodPath, SVar var) {
        MethodMockExprModel exprModel = ctx.getMethodMockInstance()
                .get(var.getExpr());
        log.trace("Handling method mock: " + exprModel.getMethod());
        List<Object> evaluatedParams = exprModel.getArgs().stream()
                .map(e -> evaluateSatisfiableExpression(methodPath, var, e))
                .collect(Collectors.toList());
        if (exprModel.throwsException())
            return new SMethodMockEvaluated(var,
                    null,
                    evaluatedParams,
                    SootInterpreter.getClass(exprModel.getExceptionType().toString()),
                    exprModel.getMethod());
        Object returnValue = exprModel.getRetVal() == null ? null :
                evaluateSatisfiableExpression(methodPath, var, exprModel.getRetVal());
        return new SMethodMockEvaluated(var,
                returnValue,
                evaluatedParams,
                null,
                exprModel.getMethod());
    }

    private Object evaluateSatisfiableExpression(SMethodPath methodPath, SVar sVar, Expr expr) {
        Status status = solver.check();
        if (status != Status.SATISFIABLE)
            throw new IllegalStateException("Unknown state: " + status);

        Model model = solver.getModel();

        Expr evalExpr = model.eval(expr, true);
        Object evaluated;
        if (SortType.MAP.equals(evalExpr.getSort())) {
            evaluated = handleMapSatisfiability(expr);
        } else if (SortType.ARRAY.equals(evalExpr.getSort())) {
            evaluated = handleListSatisfiability(expr);
        } else if (SortType.SET.equals(evalExpr.getSort())) {
            evaluated = handleSetSatisfiability(expr);
        } else if (SortType.OBJECT.equals(evalExpr.getSort())) {
            evaluated = handleObjectSatisfiability(methodPath, expr);
        } else if (SortType.NULL.equals(evalExpr.getSort())) {
            evaluated = null;
        } else if (SortType.CLASS.equals(evalExpr.getSort())) {
            evaluated = sVar.getClassType().getName()+".class";
        } else if (evalExpr.getSort().getClass() == FPSort.class) {
            // TODO: handle this
            evaluated = "0";
        } else {
//            Expr evalExpr = model.eval(expr, true);
            evaluated = evalExpr;

            if (evalExpr instanceof RatNum e) {
                evaluated = e.toDecimalString(12);
            } else if (SortType.NULL.equals(evalExpr.getSort())) {
                evaluated = null;
            }

//            if (sVar.getType() == VarType.FIELD) {
//                evalExpr = handleFieldSatisfiability(expr, evalExpr);
//            }

            // this is required to keep an accurate solver state
            BoolExpr assertion = ctx.mkEq(expr, evalExpr);
            if (!ctx.containsAssertion(assertion))
                solver.add(assertion);
        }

        log.debug(evaluated + " - " + sVar.getName());
        return evaluated;
    }

    private Expr handleFieldSatisfiability(Expr expr, Expr evalExpr) {
        Expr newExpr = ctx.mkFreshConst("freshConst", evalExpr.getSort());
        BoolExpr condition1 = ctx.mkNot(ctx.mkEq(evalExpr, newExpr));
        BoolExpr condition2 = ctx.mkEq(expr, newExpr);
        if (solver.isSatisfiable(ctx.mkAnd(condition1, condition2)))
            evalExpr = z3t.getDefaultValue(evalExpr.getSort());
        return evalExpr;
    }

    private Object handleMapSatisfiability(Expr expr) {
        Z3MapInstance mapInstance = ctx.getMapInstance();
        MapModel mapModel = mapInstance.getInitialMap(expr).orElseThrow();
        int size = solver.minimizeInteger(mapInstance.initialSize(mapModel.getReference()));
        return solver.createInitialMap(mapModel, size);
    }

    private Object handleListSatisfiability(Expr expr) {
        throw new RuntimeException("List interpretations are not yet supported");
//        Z3LinkedListInstance linkedListInstance = ctx.getLinkedListInstance();
//        LinkedListModel listModel = linkedListInstance.getInitial(expr).orElseThrow();
//        int size = solver.minimizeInteger(listModel.getSize());
//        return solver.createInitialList(listModel, size);
    }

    private Object handleSetSatisfiability(Expr expr) {
        throw new RuntimeException("Set interpretations are not yet supported");
    }

    private ClassInstanceVar handleObjectSatisfiability(SMethodPath methodPath, Expr expr) {
        ClassInstanceModel model = getClassInstanceModel(methodPath, expr);
        SClassInstance classInstance = model.getClassInstance();
        if (model.isStub()) {
            // this happens when a class is a parameter or a return value
            ClassInstanceVar classInstanceVar = new ClassInstanceVar(
                    classInstance.getClazz(), true);
            return classInstanceVar;
        }

        ClassInstanceVar classInstanceVar = new ClassInstanceVar(classInstance.getClazz());
        if (CLIOptions.shouldPropagate(classInstance.getClazz().getName())) {
            for (SVar var : classInstance.getSymbolicFieldStack().getAll()) {
                if (!var.isDeclaration()) continue;
                Object evaluated = evaluateSatisfiableExpression(methodPath, var, var.getExpr());
                classInstanceVar.getFields().put(var.getName(), evaluated);
            }
        }

        return classInstanceVar;
    }

    private ClassInstanceModel getClassInstanceModel(SMethodPath methodPath, Expr expr) {
        Optional<ClassInstanceModel> optional = ctx.getClassInstance().getInstance(expr);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            SVar sVar = methodPath.getSymbolicVarStack().get(expr)
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not get svar from expr: " + expr + " with state: " + Arrays.toString(methodPath.getSymbolicVarStack().getAll().toArray(new SVar[0]))));
            try {
                Class clazz = sVar.getClassType();
                return ctx.getClassInstance().constructor(expr, clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
