package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.*;
import sootup.core.types.ClassType;

import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);
    private final Z3Translator z3t;
    private Z3ExtendedSolver solver;
    private Z3ExtendedContext ctx;
    private final SatisfiabilityHandler satHandler;

    static {
        System.load(System.getProperty("java.library.path") + File.separator + "libz3.dylib");
        Runtime.getRuntime().addShutdownHook(new Thread(Z3Translator::close));
    }

    public SymbolicExecutor() {
        this.z3t = new Z3Translator();
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
        this.satHandler = new SatisfiabilityHandler(z3t);
    }

    public void refresh() {
        z3t.initZ3(true);
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
        this.satHandler.refresh();
    }

    public SatisfiableResults analyzeSymbolicPaths(Method method)
            throws ClassNotFoundException {
        SClassInstance classInstance = getClassInstance(method.getDeclaringClass());
        return analyzeSymbolicPaths(classInstance, method, new SParamList(), null);
    }

    public SatisfiableResults analyzeSymbolicPaths(
            SClassInstance classInstance, Executable method, SParamList paramList, JumpNode jumpNode)
            throws ClassNotFoundException {
        SMethodPath methodPathSkeleton = ctx.getClassInstance()
                .getMethodPath(classInstance, method);
        SMethodPath sMethodPath = new SMethodPath(methodPathSkeleton, paramList, jumpNode, new SStack());
        printMethod(classInstance, method);
        push(sMethodPath);
        analyzePaths(sMethodPath, sMethodPath.getRoot());
        pop(sMethodPath);
        return sMethodPath.getSatisfiableResults();
    }

    private void printMethod(SClassInstance classInstance, Executable method) {
        log.debug("Printing method: " + method.getName());
        ctx.getClassInstance().getMethodPath(classInstance, method).print();
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
//        if (type == SType.INVOKE_MOCK) {
//            mockThrowsAndPropagate(sMethodPath, node);
//        }

        Z3Status status = satHandler.checkSatisfiability(sMethodPath, node, type);
        if (Z3Status.SATISFIABLE == status)
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

//            // handle mocks
//            if (wrapped.getSType() == SType.INVOKE_MOCK) {
//                String name = node.getUnit().toString();
//                List<Expr> params = translateExpressions(method, sMethodPath);
//                Method javaMethod = (Method) SootHelper.getMethod(method.getInvokeExpr());
//                SMethodPath topMethodPath = sMethodPath.getTopMethodPath();
//                SVar sMethodMockVar = sMethodPath.getSymbolicVarStack().add(
//                        name, null, null, VarType.METHOD_MOCK, javaMethod, params);
//                if (topMethodPath != sMethodPath)
//                    topMethodPath.getSymbolicVarStack().add(sMethodMockVar);
//                return SType.INVOKE_MOCK;
//            }

            // set all values to default
            if (wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
                ClassType classType = method.getInvokeExpr().getMethodSignature().getDeclClassType();
                Expr leftOpExpr = z3t.translateValue(method.getBase(), varType, sMethodPath);
                Expr expr = ctx.getClassInstance().constructor(
                        leftOpExpr, SootHelper.getClass(classType)).getExpr();
                ctx.getClassInstance().initialize(expr);
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
            Expr expr = ctx.getClassInstance().constructor(
                    leftOpExpr, type).getExpr();
            ctx.getClassInstance().initialize(expr);
            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, sMethodPath, type);
        } else if (rightOpHolder.getSType() == SType.INVOKE_MOCK) {
            // if method cannot be invoked, then mock it.
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            Class classType = SootHelper.translateType(rightOp.getType());
            List<Expr> params = translateExpressions(methodExpr, sMethodPath);
            Method method = (Method) SootHelper.getMethod(methodExpr.getInvokeExpr());
            SMethodPath topMethodPath = sMethodPath.getTopMethodPath();
            Expr retValExpr = z3t.translateValue(rightOp, rightOpVarType, sMethodPath);
            Expr reference = ctx.getMethodMockInstance().constructor(
                    method, params, null, retValExpr).getReferenceExpr();
//            Expr expr = z3t.translateMockValue(
//                    rightOp, rightOpVarType, method, params, sMethodPath, topMethodPath);
            SMethodMockVar sMethodMockVar = new SMethodMockVar(
                    z3t.getValueName(leftOp), retValExpr, VarType.METHOD_MOCK, classType,
                    true, reference, method, params);
            topMethodPath.getSymbolicVarStack().add(sMethodMockVar);
//            SVar sMethodMockVar = z3t.updateSymbolicVar(
//                    leftOp, reference, VarType.METHOD_MOCK, topMethodPath, classType, method, params);
//            if (topMethodPath != sMethodPath)
//                topMethodPath.getSymbolicVarStack().add(sMethodMockVar);
//            z3t.updateSymbolicVar(leftOp, reference, VarType.METHOD_MOCK, topMethodPath, classType);
            return SType.INVOKE_MOCK;
        } else {
            Class classType = SootHelper.translateType(rightOp.getType());
            z3t.updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, sMethodPath, classType);
        }
        return SType.ASSIGNMENT;
    }

    private SType handleGoto(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        List<SNode> nodes = methodPath.getTargetNodes(node.getUnit());

//        node.getChildren().forEach(e -> {
//            if (!nodes.contains(e))
//                nodes.add(e);
//        });

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
        if (handlerNode != null) {
            analyzePaths(handlerNode.getMethodPath(), handlerNode.getNode());
        }
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
            methodMockVar.setThrowType(handlerNode.getNode().getExceptionType());
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
                    stmt.getOp(), assignStmt.getRightOp().getType(), VarType.RETURN_VALUE, sMethodPath);

            Value leftOp = assignStmt.getLeftOp();
            VarType leftOpVarType = getVarType(sMethodPath, leftOp);

            Class classType = SootHelper.translateType(assignStmt.getRightOp().getType());
            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, jn.getMethodPath(), classType);
        }

        List<SNode> children = jn.getNode().getChildren();
//        if (children.isEmpty()) {
//            children = jn.getMethodPath().getFollowingNodes(jn.getNode());
//        }

        for (SNode child : children)
            analyzePaths(jn.getMethodPath(), child);

        pop(sMethodPath);

        return SType.INVOKE;
    }

    private void propagate(SMethodExpr methodExpr, SMethodPath methodPath, JumpNode jumpNode)
            throws ClassNotFoundException {
        // under-approximate
        if (methodPath.getClassInstance().incrementGotoCount(jumpNode.getNode())) {
            SParamList paramList = createParamList(methodExpr, methodPath);
            Executable method = SootHelper.getMethod(methodExpr.getInvokeExpr());
            SClassInstance classInstance = getClassInstance(methodExpr.getBase(), methodPath, method);
            analyzeSymbolicPaths(classInstance, method, paramList, jumpNode);
        }
    }

    private SClassInstance getClassInstance(Value base, SMethodPath methodPath, Executable method)
            throws ClassNotFoundException {
        if (base != null && !z3t.getValueName(base).equals("this")) {
            Expr expr = z3t.translateValue(base, VarType.OTHER, methodPath);
            return ctx.getClassInstance().constructor(
                    expr, method.getDeclaringClass()).getClassInstance();
        }
        return getClassInstance(method.getDeclaringClass());
    }

    private SClassInstance getClassInstance(Class<?> clazz) throws ClassNotFoundException {
        return ctx.getClassInstance().constructor(clazz).getClassInstance();
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
        new Z3Translator();

        Z3ExtendedContext ctx = Z3Translator.getContext();
        Z3ExtendedSolver solver = ctx.getSolver();
        Z3SortUnion sortUnion = new Z3SortUnion(ctx);

        Sort arrayValueSort = sortUnion.getGenericSort();
        ArraySort arraySort = ctx.mkArraySort(ctx.mkIntSort(), arrayValueSort);

        solver.check();
        Model model = solver.getModel();
        String asd = "asdawd";
    }
}