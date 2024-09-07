package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.core.model.SootMethod;
import sootup.java.core.JavaSootClassSource;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.a1k28.evoc.helper.SootHelper.*;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);
    private final SClassInstance sClassInstance;
    private final Z3Translator z3t;
    private Z3ExtendedSolver solver;

    public SymbolicExecutor(Class<?> clazz)
            throws ClassNotFoundException {
        this.sClassInstance = createClassInstance(clazz);
        this.z3t = new Z3Translator(sClassInstance);
        this.solver = z3t.getContext().getSolver();
    }

    public void refresh() {
        Z3Translator.initZ3(true);
        this.solver = z3t.getContext().getSolver();
    }

    public SatisfiableResults analyzeSymbolicPaths(Method method)
            throws ClassNotFoundException {
        return analyzeSymbolicPaths(method, new SParamList(), null);
    }

    public SatisfiableResults analyzeSymbolicPaths(Method method, SParamList paramList, JumpNode jumpNode)
            throws ClassNotFoundException {
        SMethodPath methodPathSkeleton = sClassInstance.getMethodPathSkeletons().get(method);
        SMethodPath sMethodPath = new SMethodPath(methodPathSkeleton, paramList, jumpNode, new SStack());
        printMethod(method);
        push(sMethodPath);
        analyzePaths(sMethodPath, sMethodPath.getRoot());
        pop(sMethodPath);
        return sMethodPath.getSatisfiableResults();
    }

    private SClassInstance createClassInstance(Class<?> clazz)
            throws ClassNotFoundException {
        SootClass<JavaSootClassSource> sootClass = getSootClass(clazz.getName());
        SClassInstance instance = new SClassInstance(clazz, null);

        List<String> fields = SootHelper.getFields(sootClass).stream()
                .map(SootClassMember::toString).toList();
        instance.getFields().addAll(fields);

        for (Method method : clazz.getDeclaredMethods()) {
            SMethodPath sMethodPath = createMethodPath(sootClass, method);
            instance.getMethodPathSkeletons().put(method, sMethodPath);
        }

        return instance;
    }

    private SMethodPath createMethodPath(SootClass<JavaSootClassSource> sootClass, Method method) {
        // Find all paths
        SootMethod sootMethod = getSootMethod(sootClass, method);
        Body body = sootMethod.getBody();
        SMethodPath sMethodPath = new SMethodPath(body, method);
        createFlowDiagram(sMethodPath, body);
        return sMethodPath;
    }

    private void printMethod(Method method) {
        log.debug("Printing method: " + method.getName());
        sClassInstance.getMethodPathSkeletons().get(method).print();
    }

    private void analyzePaths(SMethodPath sMethodPath, SNode node) throws ClassNotFoundException {
        SType type = null;
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE){
            push(sMethodPath);
        }

        // handle node types
        if (node.getType() == SType.PARAMETER) {
            if (sMethodPath.getParamList().hasNext())
                updateParameter(node.getUnit(), sMethodPath);
            else
                saveParameter(node.getUnit(), sMethodPath);
        }
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            handleBranch(sMethodPath, node);
        }
        if (node.getType() == SType.ASSIGNMENT) {
            type = handleAssignment(sMethodPath, node);
        }
        if (node.getType() == SType.INVOKE) {
            type = handleVoidMethodCall(sMethodPath, node);
        }
        if (node.getType() == SType.GOTO) {
            List<SNode> nodes = sMethodPath.getSNodes(node.getUnit());
            assert nodes.size() == 1; // TODO: is this always 1??

            // under-approximate
            if (sClassInstance.incrementGotoCount(node))
                analyzePaths(sMethodPath, nodes.get(0));
        }
        if (node.getType() == SType.RETURN
                || node.getType() == SType.RETURN_VOID
                || node.getType() == SType.THROW) {
            // TODO: handle throw clause
            type = handleReturn(sMethodPath, node);
        }

        if (Z3Status.SATISFIABLE == checkSatisfiability(sMethodPath, node, type))
            for (SNode child : node.getChildren())
                analyzePaths(sMethodPath, child);

        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            pop(sMethodPath);
        }
    }

    private void handleBranch(SMethodPath sMethodPath, SNode node) {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        BoolExpr z3Condition = (BoolExpr) z3t.translateCondition(
                condition, getVarType(ifStmt), sMethodPath.getSymbolicVarStack());
        BoolExpr assertion = node.getType() == SType.BRANCH_TRUE ? z3Condition : z3t.mkNot(z3Condition);
        solver.add(assertion);
    }

    private SType handleVoidMethodCall(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        JInvokeStmt invoke = (JInvokeStmt) node.getUnit();
        SExpr wrapped = z3t.wrapMethodCall(invoke.getInvokeExpr(), sMethodPath.getSymbolicVarStack());
        if (wrapped.getSType() != SType.INVOKE) return wrapped.getSType();
        SMethodExpr method = wrapped.asMethod();
        if (!method.shouldPropagate()) {
            z3t.callProverMethod(method, getVarType(invoke), sMethodPath.getSymbolicVarStack());
            return SType.OTHER;
        }
        JumpNode jumpNode = new JumpNode(sMethodPath, node);
        propagate(method, sMethodPath, jumpNode);
        return SType.INVOKE;
    }

    private SType handleAssignment(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        SStack stack = sMethodPath.getSymbolicVarStack();

        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        VarType leftOpVarType = getVarType(leftOp);
        VarType rightOpVarType = getVarType(rightOp);
        SExpr rightOpHolder = z3t.translateAndWrapValue(rightOp, rightOpVarType, stack);

        if (rightOpHolder.getSType() == SType.INVOKE) {
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            if (methodExpr.shouldPropagate()) {
                JumpNode jumpNode = new JumpNode(sMethodPath, node);
                propagate(methodExpr, sMethodPath, jumpNode);
                return SType.INVOKE;
            } else {
                Expr expr = z3t.callProverMethod(rightOpHolder.asMethod(), rightOpVarType, stack);
                z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, stack);
            }
        } else {
            // if method cannot be invoked, then mock it.
            if (rightOpVarType == VarType.METHOD) leftOpVarType = VarType.METHOD_MOCK;
            z3t.updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, stack);
        }
        return SType.ASSIGNMENT;
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
                    stmt.getOp(), VarType.RETURN_VALUE, sMethodPath.getSymbolicVarStack());

            Value leftOp = assignStmt.getLeftOp();
            VarType leftOpVarType = getVarType(leftOp);

            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, jn.getMethodPath().getSymbolicVarStack());
        }

        for (SNode child : jn.getNode().getChildren())
            analyzePaths(jn.getMethodPath(), child);

        pop(sMethodPath);

        return SType.INVOKE;
    }

    private void propagate(SMethodExpr methodExpr, SMethodPath methodPath, JumpNode jumpNode)
            throws ClassNotFoundException {
        // under-approximate
        if (!sClassInstance.incrementGotoCount(jumpNode.getNode()))
            return;
        List<Value> args = methodExpr.getArgs();
        List<Expr> exprArgs = args.stream()
                .map(e -> z3t.translateValue(e, getVarType(e), methodPath.getSymbolicVarStack()))
                .collect(Collectors.toList());
        Method method = SootHelper.getMethod(methodExpr.getInvokeExpr());
        analyzeSymbolicPaths(method, new SParamList(exprArgs), jumpNode);
    }

    private Z3Status checkSatisfiability(SMethodPath sMethodPath, SNode node, SType type) {
        if (solver.check() != Status.SATISFIABLE) {
            log.warn("Path is unsatisfiable - " + sMethodPath.getMethod().getName() + "\n");
            return Z3Status.UNSATISFIABLE_END;
        }
        if (SType.INVOKE == type) return Z3Status.SATISFIABLE_END;
        if (SType.RETURN == type || SType.RETURN_VOID == type || SType.THROW == type) {
            // if tail
            if (node.getChildren().isEmpty()) {
                SVarEvaluated returnValue = null;
                if (node.getType() == SType.RETURN) {
                    JReturnStmt stmt = (JReturnStmt) node.getUnit();
                    Expr expr = z3t.translateValue(
                            stmt.getOp(), VarType.RETURN_VALUE, sMethodPath.getSymbolicVarStack());
                    Class<?> classType = SootHelper.translateType(stmt.getOp().getType());
                    SVar svar = new SVar(z3t.getValueName(stmt.getOp()),
                            stmt.getOp(),
                            expr,
                            VarType.RETURN_VALUE,
                            classType,
                            true);
                    returnValue = new SVarEvaluated(
                            svar,
                            solver.getModel().eval(expr, true).toString());
                }
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
        List<SVar> symbolicVars = sMethodPath.getSymbolicVarStack().getAll();
        symbolicVars.addAll(sClassInstance.getSymbolicFieldStack().getAll());
        for (SVar var : symbolicVars) {
            if (!var.isDeclaration()) continue;
            if (var.getType() != VarType.PARAMETER
                    && var.getType() != VarType.FIELD
                    && var.getType() != VarType.METHOD_MOCK) continue;

            if (solver.check() != Status.SATISFIABLE)
                throw new IllegalStateException("Unknown state: " + solver.check());

            Model model = solver.getModel();

            Expr evaluated = model.eval(var.getExpr(), true);
            log.debug(evaluated + " - " + var.getName());

            // this is required to keep an accurate solver state
            solver.add(z3t.mkEq(var.getExpr(), evaluated));

            SVarEvaluated sVarEvaluated;
            if (SortType.MAP.equals(var.getExpr().getSort())) {
                sVarEvaluated = handleMapSatisfiability(var);
            } else {
                sVarEvaluated = new SVarEvaluated(var, evaluated.toString());
            }

            if (var.getType() == VarType.FIELD) fieldsEvaluated.add(sVarEvaluated);
            else parametersEvaluated.add(sVarEvaluated);
        }

        if (returnValue != null)
            log.debug(returnValue.getEvaluated() + " - ret. val.");

        // TODO: handle throw clause
        SatisfiableResult satisfiableResult = new SatisfiableResult(
                solver.getAssertions(),
                fieldsEvaluated,
                parametersEvaluated,
                returnValue,
                true
        );

        sMethodPath.getSatisfiableResults().getResults().add(satisfiableResult);
        log.empty();
    }

    private SVarEvaluated handleMapSatisfiability(SVar var) {
        Optional<MapModel> mapModelOptional = Z3Translator.getContext().getInitialMap(var.getExpr());
        if (mapModelOptional.isEmpty()) return new SVarEvaluated(var, new HashMap<>());
        MapModel mapModel = mapModelOptional.get();
        int size = solver.minimizeInteger(mapModel.getSize());
        Map map = solver.createInitialMap(mapModel, size);
        return new SVarEvaluated(var, map);
    }

    private void saveParameter(Stmt unit, SMethodPath methodPath) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        z3t.saveSymbolicVar(
                ref,
                ref.getType(),
                getVarType(unit),
                methodPath.getSymbolicVarStack());
    }

    private void updateParameter(Stmt unit, SMethodPath methodPath) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        z3t.updateSymbolicVar(ref,
                methodPath.getParamList().getNext(),
                getVarType(unit),
                methodPath.getSymbolicVarStack());
    }

    private VarType getVarType(Stmt unit) {
        String val = unit.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (sClassInstance.getFields().contains(val)) return VarType.FIELD;
        return VarType.getType(unit);
    }

    private VarType getVarType(Value value) {
        String val = value.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (sClassInstance.getFields().contains(val)) return VarType.FIELD;
        return VarType.getType(value);
    }

    private void push(SMethodPath sMethodPath) {
        solver.push();
        sMethodPath.getSymbolicVarStack().push();
        sClassInstance.getSymbolicFieldStack().push();
        z3t.getStack().push();
    }

    private void pop(SMethodPath sMethodPath) {
        solver.pop();
        sMethodPath.getSymbolicVarStack().pop();
        sClassInstance.getSymbolicFieldStack().pop();
        z3t.getStack().pop();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
//        new SymbolicPathCarver("com.github.a1k28.Stack", "test_method_call")
//                .analyzeSymbolicPaths();
//        close();

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