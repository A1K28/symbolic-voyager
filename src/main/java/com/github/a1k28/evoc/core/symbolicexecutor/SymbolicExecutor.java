package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.ClassInstanceModel;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.*;

import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);
    private final Z3Translator z3t;
    private Z3ExtendedSolver solver;

    static {
        System.load(System.getProperty("java.library.path") + File.separator + "libz3.dylib");
        Runtime.getRuntime().addShutdownHook(new Thread(Z3Translator::close));
    }

    public SymbolicExecutor() {
        this.z3t = new Z3Translator();
        this.solver = z3t.getContext().getSolver();
    }

    public void refresh() {
        Z3Translator.initZ3(true);
        this.solver = z3t.getContext().getSolver();
    }

    public SatisfiableResults analyzeSymbolicPaths(Method method)
            throws ClassNotFoundException {
        SClassInstance classInstance = getClassInstance(method.getDeclaringClass());
        return analyzeSymbolicPaths(classInstance, method, new SParamList(), null);
    }

    public SatisfiableResults analyzeSymbolicPaths(
            SClassInstance classInstance, Executable method, SParamList paramList, JumpNode jumpNode)
            throws ClassNotFoundException {
        SMethodPath methodPathSkeleton = z3t.getContext().getClassMethodPath(classInstance, method);
        SMethodPath sMethodPath = new SMethodPath(methodPathSkeleton, paramList, jumpNode, new SStack());
        printMethod(classInstance, method);
        push(sMethodPath);
        analyzePaths(sMethodPath, sMethodPath.getRoot());
        pop(sMethodPath);
        return sMethodPath.getSatisfiableResults();
    }

    private void printMethod(SClassInstance classInstance, Executable method) {
        log.debug("Printing method: " + method.getName());
        z3t.getContext().getClassMethodPath(classInstance, method).print();
    }

    private void analyzePaths(SMethodPath sMethodPath, SNode node) throws ClassNotFoundException {
        SType type = null;
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE){
            push(sMethodPath);
        }

        // handle node types
        if (node.getType() == SType.PARAMETER) {
            type = handleParameter(sMethodPath, node);
        }
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            type = handleBranch(sMethodPath, node);
        }
        if (node.getType() == SType.ASSIGNMENT) {
            type = handleAssignment(sMethodPath, node);
        }
        if (node.getType() == SType.INVOKE) {
            type = handleVoidMethodCall(sMethodPath, node);
        }
        if (node.getType() == SType.GOTO) {
            type = handleGoto(sMethodPath, node);
        }
        if (node.getType() == SType.RETURN || node.getType() == SType.RETURN_VOID) {
            type = handleReturn(sMethodPath, node);
        }
        if (node.getType() == SType.THROW) {
            type = handleThrows(sMethodPath, node);
        }

        // expand paths by allowing method mocks to throw exceptions
        if (type == SType.INVOKE_MOCK) {
            mockThrowsAndPropagate(sMethodPath, node);
        }

        if (Z3Status.SATISFIABLE == checkSatisfiability(sMethodPath, node, type))
            for (SNode child : node.getChildren())
                analyzePaths(sMethodPath, child);

        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            pop(sMethodPath);
        }
    }

    private SType handleParameter(SMethodPath sMethodPath, SNode node) {
        if (sMethodPath.getParamList().hasNext())
            updateParameter(sMethodPath, node.getUnit());
        else
            saveParameter(sMethodPath, node.getUnit());
        return SType.PARAMETER;
    }

    private SType handleBranch(SMethodPath sMethodPath, SNode node) {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        BoolExpr z3Condition = (BoolExpr) z3t.translateCondition(
                condition, getVarType(sMethodPath, ifStmt), sMethodPath);
        BoolExpr assertion = node.getType() == SType.BRANCH_TRUE ? z3Condition : z3t.mkNot(z3Condition);
        solver.add(assertion);
        return SType.BRANCH;
    }

    private SType handleVoidMethodCall(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        JInvokeStmt invoke = (JInvokeStmt) node.getUnit();
        SExpr wrapped = z3t.wrapMethodCall(invoke.getInvokeExpr(), null);
        VarType varType = getVarType(sMethodPath, invoke);

        if (wrapped.getSType() == SType.INVOKE
                || wrapped.getSType() == SType.INVOKE_MOCK
                || wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
            SMethodExpr method = wrapped.asMethod();
            if (method.getPropagationType() == MethodPropagationType.MODELLED) {
                z3t.callProverMethod(method, varType, sMethodPath);
                return SType.OTHER;
            }

            // handle mocks
            if (wrapped.getSType() == SType.INVOKE_MOCK) {
                String name = node.getUnit().toString();
                List<Expr> params = translateExpressions(method, sMethodPath);
                Method javaMethod = (Method) SootHelper.getMethod(method.getInvokeExpr());
                sMethodPath.getSymbolicVarStack().add(
                        name, null, null, VarType.METHOD_MOCK, javaMethod, params);
                return SType.INVOKE_MOCK;
            }

            // set all values to default
            if (wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
                Expr leftOpExpr = z3t.translateValue(method.getBase(), varType, sMethodPath);
                z3t.getContext().mkClassInitialize(leftOpExpr);
            }

            JumpNode jumpNode = new JumpNode(sMethodPath, node);
            propagate(method, sMethodPath, jumpNode);
            return SType.INVOKE;
        }

        return wrapped.getSType();
    }

    private SType handleAssignment(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        VarType leftOpVarType = getVarType(sMethodPath, leftOp);
        VarType rightOpVarType = getVarType(sMethodPath, rightOp);
        SExpr rightOpHolder = z3t.translateAndWrapValue(rightOp, rightOpVarType, sMethodPath);

        if (rightOpHolder.getSType() == SType.INVOKE) {
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            if (methodExpr.getPropagationType() == MethodPropagationType.PROPAGATE) {
                JumpNode jumpNode = new JumpNode(sMethodPath, node);
                propagate(methodExpr, sMethodPath, jumpNode);
                return SType.INVOKE;
            } else {
                Class<?> type = SootHelper.translateType(rightOp.getType());
                Expr expr = z3t.callProverMethod(rightOpHolder.asMethod(), rightOpVarType, sMethodPath);
                z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, sMethodPath, type);
            }
        } else if (rightOpHolder.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
            Class<?> type = SootHelper.translateType(rightOp.getType());
            Expr leftOpExpr = z3t.translateValue(leftOp, leftOpVarType, sMethodPath);
            Expr expr = z3t.getContext().mkClassInstance(leftOpExpr, type).getExpr();
            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, sMethodPath, type);
        } else if (rightOpHolder.getSType() == SType.INVOKE_MOCK) {
            // if method cannot be invoked, then mock it.
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            Class classType = SootHelper.translateType(rightOp.getType());
            Expr expr = z3t.translateValue(rightOp, rightOpVarType, sMethodPath);
            List<Expr> params = translateExpressions(methodExpr, sMethodPath);
            Method method = (Method) SootHelper.getMethod(methodExpr.getInvokeExpr());
            z3t.updateSymbolicVar(leftOp, expr, VarType.METHOD_MOCK, sMethodPath, classType, method, params);
            return SType.INVOKE_MOCK;
        } else {
            Class classType = SootHelper.translateType(rightOp.getType());
            z3t.updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, sMethodPath, classType);
        }
        return SType.ASSIGNMENT;
    }

    private SType handleGoto(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        List<SNode> nodes = methodPath.getSNodes(node.getUnit());
        assert nodes.size() == 1; // TODO: is this always 1??

        node.getChildren().forEach(e -> {
            if (!nodes.contains(e))
                nodes.add(e);
        });

        // under-approximate
        for (SNode n : nodes) {
            if (methodPath.getClassInstance().incrementGotoCount(node)) {
                analyzePaths(methodPath, n);
            }
        }

        return SType.GOTO;
    }

    private SType handleThrows(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        push(sMethodPath);

        Class exceptionType = sMethodPath.getSymbolicVarStack()
                .get(z3t.getValueName(((JThrowStmt) node.getUnit()).getOp())).get()
                .getClassType();
        HandlerNode handlerNode = sMethodPath.getHandlerNode(node, exceptionType);
        analyzePaths(handlerNode.getMethodPath(), handlerNode.getNode());

        pop(sMethodPath);

        return SType.THROW;
    }

    private void mockThrowsAndPropagate(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        SMethodMockVar methodMockVar;

        if (node.getUnit() instanceof JAssignStmt assignStmt) {
            methodMockVar = (SMethodMockVar) sMethodPath.getSymbolicVarStack()
                    .get(z3t.getValueName(assignStmt.getLeftOp())).orElseThrow();
        } else {
            methodMockVar = (SMethodMockVar) sMethodPath.getSymbolicVarStack()
                    .get(node.getUnit().toString()).orElseThrow();
        }

        for (HandlerNode handlerNode : sMethodPath.getHandlerNodes(node)) {
            push(sMethodPath);
            methodMockVar.setThrowType(handlerNode.getTrap().getExceptionType());
            analyzePaths(handlerNode.getMethodPath(), handlerNode.getNode());
            pop(sMethodPath);
        }

        // clear throws so that further paths assume a return value (if non-void)
        methodMockVar.setThrowType(null);
    }

    private SType handleReturn(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        if (sMethodPath.getJumpNode() == null)
            return node.getType();

        push(sMethodPath);

        JumpNode jn = sMethodPath.getJumpNode();
        if (node.getType() == SType.RETURN
                && jn.getNode().getUnit() instanceof JAssignStmt<?, ?> assignStmt) {
            JReturnStmt stmt = (JReturnStmt) node.getUnit();
            Expr expr = z3t.translateValue(
                    stmt.getOp(), VarType.RETURN_VALUE, sMethodPath);

            Value leftOp = assignStmt.getLeftOp();
            VarType leftOpVarType = getVarType(sMethodPath, leftOp);

            Class classType = SootHelper.translateType(assignStmt.getRightOp().getType());
            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, jn.getMethodPath(), classType);
        }

        for (SNode child : jn.getNode().getChildren())
            analyzePaths(jn.getMethodPath(), child);

        pop(sMethodPath);

        return SType.INVOKE;
    }

    private void propagate(SMethodExpr methodExpr, SMethodPath methodPath, JumpNode jumpNode)
            throws ClassNotFoundException {
        // under-approximate
        if (!methodPath.getClassInstance().incrementGotoCount(jumpNode.getNode()))
            return;
        SParamList paramList = createParamList(methodExpr, methodPath);
        Executable method = SootHelper.getMethod(methodExpr.getInvokeExpr());
        SClassInstance classInstance = getClassInstance(methodExpr.getBase(), methodPath, method);
        analyzeSymbolicPaths(classInstance, method, paramList, jumpNode);
    }

    private SParamList createParamList(SMethodExpr methodExpr, SMethodPath methodPath) {
        List<Value> args = methodExpr.getArgs();
        List<Expr> exprArgs = args.stream()
                .map(e -> z3t.translateValue(e, getVarType(methodPath, e), methodPath))
                .collect(Collectors.toList());
        List<Class> types = args.stream()
                .map(e -> SootHelper.translateType(e.getType()))
                .collect(Collectors.toList());
        return new SParamList(exprArgs, types);
    }

    private List<Expr> translateExpressions(SMethodExpr methodExpr, SMethodPath methodPath) {
        List<Value> args = methodExpr.getArgs();
        return args.stream()
                .map(e -> z3t.translateValue(e, getVarType(methodPath, e), methodPath))
                .collect(Collectors.toList());
    }

    private Z3Status checkSatisfiability(SMethodPath sMethodPath, SNode node, SType type) {
        if (solver.check() != Status.SATISFIABLE) {
            log.warn("Path is unsatisfiable - " + sMethodPath.getMethod().getName() + "\n");
            return Z3Status.UNSATISFIABLE_END;
        }
        if (SType.INVOKE == type || SType.GOTO == type || SType.THROW == type)
            return Z3Status.SATISFIABLE_END;
        if (SType.RETURN == type || SType.RETURN_VOID == type) {
            // if tail
            if (node.getChildren().isEmpty()) {
                SVarEvaluated returnValue = getReturnValue(sMethodPath, node);
                handleSatisfiability(sMethodPath, returnValue);
                return Z3Status.SATISFIABLE_END;
            }
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
                Object evaluated = evaluateSatisfiableExpression(var.getExpr(), var.getName());
                SVarEvaluated sVarEvaluated = new SVarEvaluated(var, evaluated);
                fieldsEvaluated.add(sVarEvaluated);
            } else if (var.getType() == VarType.METHOD_MOCK) {
                SMethodMockEvaluated sVarEvaluated = handleMockExpression(var, var.getExpr());
                if (sVarEvaluated != null) mockedMethodsEvaluated.add(sVarEvaluated);
            } else {
                // parameter
                Object evaluated = evaluateSatisfiableExpression(var.getExpr(), var.getName());
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
            Class<?> classType = SootHelper.translateType(stmt.getOp().getType());
            SVar svar = new SVar(z3t.getValueName(stmt.getOp()),
                    expr,
                    VarType.RETURN_VALUE,
                    classType,
                    true);
            Object evaluated = evaluateSatisfiableExpression(expr);
            return new SVarEvaluated(svar, evaluated);
        }
        return null;
    }

    private SMethodMockEvaluated handleMockExpression(SVar var, Expr returnExpr) {
        SMethodMockVar sMethodMockVar = (SMethodMockVar) var;
        List<Object> evaluatedParams = sMethodMockVar.getArguments().stream()
                .map(this::evaluateSatisfiableExpression)
                .collect(Collectors.toList());
        if (sMethodMockVar.getThrowType() != null)
            return new SMethodMockEvaluated(var,
                    null,
                    evaluatedParams,
                    SootHelper.getClass(sMethodMockVar.getThrowType()),
                    sMethodMockVar.getMethod());
        if (returnExpr == null) return null;
        Object returnValue = evaluateSatisfiableExpression(returnExpr);
        return new SMethodMockEvaluated(var,
                returnValue,
                evaluatedParams,
                null,
                sMethodMockVar.getMethod());
    }

    private Object evaluateSatisfiableExpression(Expr expr, String name) {
        Object evaluated = evaluateSatisfiableExpression(expr);
        log.debug(evaluated + " - " + name);
        return evaluated;
    }


    private Object evaluateSatisfiableExpression(Expr expr) {
        if (solver.check() != Status.SATISFIABLE)
            throw new IllegalStateException("Unknown state: " + solver.check());
        Model model = solver.getModel();

        Object evaluated;
        if (SortType.MAP.equals(expr.getSort())) {
            evaluated = handleMapSatisfiability(expr);
        } else if (SortType.OBJECT.equals(expr.getSort())) {
            evaluated = handleObjectSatisfiability(expr);
        } else {
            evaluated = model.eval(expr, true);
            // this is required to keep an accurate solver state
            BoolExpr assertion = z3t.mkEq(expr, (Expr) evaluated);
            if (!z3t.containsAssertion(assertion))
                solver.add(assertion);
        }

        return evaluated;
    }

    private Object handleMapSatisfiability(Expr expr) {
        MapModel mapModel = z3t.getContext().getInitialMap(expr).orElseThrow();
        int size = solver.minimizeInteger(mapModel.getSize());
        return solver.createInitialMap(mapModel, size);
    }

    private Object handleObjectSatisfiability(Expr expr) {
        ClassInstanceModel model = z3t.getContext().getClassInstance(expr).orElseThrow();
        SClassInstance classInstance = model.getClassInstance();
        ClassInstanceVar classInstanceVar = new ClassInstanceVar(classInstance.getClazz());
        for (SVar sVar : classInstance.getSymbolicFieldStack().getAll()) {
            if (!sVar.isDeclaration()) continue;
            Object evaluated = evaluateSatisfiableExpression(sVar.getExpr());
            classInstanceVar.getFields().put(sVar.getName(), evaluated);
        }
        return classInstanceVar;
    }

    private SClassInstance getClassInstance(Value base, SMethodPath methodPath, Executable method)
            throws ClassNotFoundException {
        if (base != null && !z3t.getValueName(base).equals("this")) {
            Expr expr = z3t.translateValue(base, VarType.OTHER, methodPath);
            return z3t.getContext().mkClassInstance(expr, method.getDeclaringClass()).getClassInstance();
        }
        return getClassInstance(method.getDeclaringClass());
    }

    private SClassInstance getClassInstance(Class<?> clazz) throws ClassNotFoundException {
        return z3t.getContext().mkClassInstance(clazz).getClassInstance();
    }

    private void saveParameter(SMethodPath methodPath, Stmt unit) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        z3t.saveSymbolicVar(
                ref,
                ref.getType(),
                getVarType(methodPath, unit),
                methodPath);
    }

    private void updateParameter(SMethodPath methodPath, Stmt unit) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        SParamList.Param param = methodPath.getParamList().getNext();
        z3t.updateSymbolicVar(ref,
                param.getExpression(),
                getVarType(methodPath, unit),
                methodPath);
    }

    private VarType getVarType(SMethodPath methodPath, Stmt unit) {
        String val = unit.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (methodPath.getClassInstance().getFieldNames().contains(val)) return VarType.FIELD;
        return VarType.getType(unit);
    }

    private VarType getVarType(SMethodPath methodPath, Value value) {
        String val = value.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (methodPath.getClassInstance().getFieldNames().contains(val)) return VarType.FIELD;
        return VarType.getType(value);
    }

    private void push(SMethodPath sMethodPath) {
        solver.push();
        sMethodPath.getSymbolicVarStack().push();
        sMethodPath.getClassInstance().getSymbolicFieldStack().push();
        z3t.getStack().push();
    }

    private void pop(SMethodPath sMethodPath) {
        solver.pop();
        sMethodPath.getSymbolicVarStack().pop();
        sMethodPath.getClassInstance().getSymbolicFieldStack().pop();
        z3t.getStack().pop();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Context ctx = new Context();
        Solver solver = ctx.mkSolver();

        ArraySort arraySort = ctx.mkArraySort(ctx.mkStringSort(), ctx.mkStringSort());
        ArrayExpr arr1 = (ArrayExpr) ctx.mkFreshConst("arr1", arraySort);

        Expr symbolicKey = ctx.mkConst("symbolicKey", ctx.mkStringSort());
        arr1 = ctx.mkStore(arr1, symbolicKey, ctx.mkString("ASD"));

        BoolExpr exists = ctx.mkEq(ctx.mkSelect(arr1, ctx.mkString("ASD")), ctx.mkString("ASD"));

        solver.add(exists);

        Status status = solver.check();
        Model model = solver.getModel();

//        System.out.println(model.eval());

        String asd = "asdawd";
    }
}