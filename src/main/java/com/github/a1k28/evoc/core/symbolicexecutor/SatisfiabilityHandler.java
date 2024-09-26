package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.instance.Z3LinkedListInstance;
import com.github.a1k28.evoc.core.z3extended.instance.Z3MapInstance;
import com.github.a1k28.evoc.core.z3extended.model.*;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.core.sootup.SootInterpreter;
import com.microsoft.z3.*;
import sootup.core.jimple.common.stmt.JReturnStmt;

import java.util.ArrayList;
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
            log.warn("Path is unsatisfiable - " + sMethodPath.getMethod().getName() + "\n");
            return Z3Status.UNSATISFIABLE_END;
        }
        if (SType.INVOKE == type || SType.GOTO == type || SType.THROW == type)
            return Z3Status.SATISFIABLE_END;
        if (SType.RETURN == type || SType.RETURN_VOID == type) {
            // if tail
            SVarEvaluated returnValue = getReturnValue(sMethodPath, node);
            handleSatisfiability(sMethodPath, returnValue);
            return Z3Status.SATISFIABLE_END;
        }
        return Z3Status.SATISFIABLE;
    }

    private void handleSatisfiability(SMethodPath sMethodPath, SVarEvaluated returnValue) {
        log.info("Path is satisfiable - " + sMethodPath.getMethod().getName());

        List<SVarEvaluated> fieldsEvaluated = new ArrayList<>();
        List<SVarEvaluated> parametersEvaluated = new ArrayList<>();
        List<SMethodMockEvaluated> mockedMethodsEvaluated = new ArrayList<>();
        List<SVar> symbolicVars = sMethodPath.getSymbolicVarStack().getAll();
        symbolicVars.addAll(sMethodPath.getClassInstance().getSymbolicFieldStack().getAll());
        for (SVar var : symbolicVars) {
            if (!var.isDeclaration()) continue;
            if (var.getType() != VarType.PARAMETER
                    && var.getType() != VarType.FIELD
                    && var.getType() != VarType.METHOD_MOCK) continue;

            if (var.getType() == VarType.FIELD) {
                Object evaluated = evaluateSatisfiableExpression(
                        sMethodPath, var.getExpr(), var.getName());
                SVarEvaluated sVarEvaluated = new SVarEvaluated(var, evaluated);
                fieldsEvaluated.add(sVarEvaluated);
            } else if (var.getType() == VarType.METHOD_MOCK) {
                SMethodMockEvaluated sVarEvaluated = handleMockExpression(sMethodPath, var);
                mockedMethodsEvaluated.add(sVarEvaluated);
            } else {
                // parameter
                Object evaluated = evaluateSatisfiableExpression(
                        sMethodPath, var.getExpr(), var.getName());
                SVarEvaluated sVarEvaluated = new SVarEvaluated(var, evaluated);
                parametersEvaluated.add(sVarEvaluated);
            }
        }

        if (returnValue != null)
            log.debug(returnValue.getEvaluated() + " - ret. val.");

        // TODO: handle throw clause
        SatisfiableResult satisfiableResult = new SatisfiableResult(
                solver.getAssertions(),
                fieldsEvaluated,
                parametersEvaluated,
                mockedMethodsEvaluated,
                returnValue,
                true
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
            Object evaluated = evaluateSatisfiableExpression(methodPath, expr);
            return new SVarEvaluated(svar, evaluated);
        }
        return null;
    }

    private SMethodMockEvaluated handleMockExpression(SMethodPath methodPath, SVar var) {
        MethodMockExprModel exprModel = ctx.getMethodMockInstance()
                .get(var.getExpr());
        List<Object> evaluatedParams = exprModel.getArgs().stream()
                .map(e -> evaluateSatisfiableExpression(methodPath, e))
                .collect(Collectors.toList());
        if (exprModel.throwsException())
            return new SMethodMockEvaluated(var,
                    null,
                    evaluatedParams,
                    SootInterpreter.getClass(exprModel.getExceptionType().toString()),
                    exprModel.getMethod());
        Object returnValue = exprModel.getRetVal() == null ? null :
                evaluateSatisfiableExpression(methodPath, exprModel.getRetVal());
        return new SMethodMockEvaluated(var,
                returnValue,
                evaluatedParams,
                null,
                exprModel.getMethod());
    }

    private Object evaluateSatisfiableExpression(SMethodPath methodPath, Expr expr, String name) {
        Object evaluated = evaluateSatisfiableExpression(methodPath, expr);
        log.debug(evaluated + " - " + name);
        return evaluated;
    }


    private Object evaluateSatisfiableExpression(SMethodPath methodPath, Expr expr) {
        Status status = solver.check();
        if (status != Status.SATISFIABLE)
            throw new IllegalStateException("Unknown state: " + status);
        Model model = solver.getModel();

        Object evaluated;
        if (SortType.MAP.equals(expr.getSort())) {
            evaluated = handleMapSatisfiability(expr);
        } else if (SortType.ARRAY.equals(expr.getSort())) {
            evaluated = handleListSatisfiability(expr);
        } else if (SortType.SET.equals(expr.getSort())) {
            evaluated = handleSetSatisfiability(expr);
        } else if (SortType.OBJECT.equals(expr.getSort())) {
            evaluated = handleObjectSatisfiability(methodPath, expr);
        } else {
            evaluated = model.eval(expr, true);
            // this is required to keep an accurate solver state
            BoolExpr assertion = ctx.mkEq(expr, (Expr) evaluated);
            if (!ctx.containsAssertion(assertion))
                solver.add(assertion);
        }

        return evaluated;
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

    private Object handleObjectSatisfiability(SMethodPath methodPath, Expr expr) {
        ClassInstanceModel model = getClassInstanceModel(methodPath, expr);
        SClassInstance classInstance = model.getClassInstance();
        ClassInstanceVar classInstanceVar = new ClassInstanceVar(classInstance.getClazz());
        for (SVar sVar : classInstance.getSymbolicFieldStack().getAll()) {
            if (!sVar.isDeclaration()) continue;
            Object evaluated = evaluateSatisfiableExpression(methodPath, sVar.getExpr());
            classInstanceVar.getFields().put(sVar.getName(), evaluated);
        }
        return classInstanceVar;
    }

    private ClassInstanceModel getClassInstanceModel(SMethodPath methodPath, Expr expr) {
        SVar sVar = methodPath.getSymbolicVarStack().get(expr).orElseThrow();
        Optional<ClassInstanceModel> optional = ctx.getClassInstance().getInstance(expr);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            try {
                Class clazz = sVar.getClassType();
                return ctx.getClassInstance().constructor(expr, clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
