package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.*;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.*;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import com.github.a1k28.symvoyager.core.z3extended.model.SortType;
import com.github.a1k28.symvoyager.helper.Logger;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.*;
import sootup.core.types.ClassType;
import sootup.core.types.Type;

import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import static com.github.a1k28.symvoyager.helper.OSDependentZ3Loader.loadZ3Library;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);
    private final Z3Translator z3t;
    private Z3ExtendedSolver solver;
    private Z3ExtendedContext ctx;
    private LinkedList<SNode> path;
    private final SatisfiabilityHandler satHandler;
    private int depth = 0;

    static {
        loadZ3Library(System.getProperty("java.library.path") + File.separator);
        Runtime.getRuntime().addShutdownHook(new Thread(Z3Translator::close));
    }

    public SymbolicExecutor() {
        this.z3t = new Z3Translator();
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
        this.satHandler = new SatisfiabilityHandler(z3t);
        this.path = new LinkedList<>();
    }

    public void refresh() {
        z3t.initZ3(true);
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
        this.satHandler.refresh();
        this.path = new LinkedList<>();
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
        SMethodPath sMethodPath = new SMethodPath(
                methodPathSkeleton, paramList, jumpNode);
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

        path.add(node);

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

        Z3Status status = satHandler.checkSatisfiability(sMethodPath, node, type, path);
        if (Z3Status.SATISFIABLE == status)
            for (SNode child : node.getChildren())
                analyzePaths(sMethodPath, child);

        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            pop(sMethodPath);
        }

        path.removeLast();
    }

    private SType handleParameter(SMethodPath sMethodPath, SNode node) {
        if (sMethodPath.getParamList().hasNext()) { // is inner method
            updateParameter(sMethodPath, node.getUnit());
        } else { // is target/first method
            saveParameter(sMethodPath, node.getUnit());
        }
        return SType.PARAMETER;
    }

    private SType handleBranch(SMethodPath sMethodPath, SNode node) {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        BoolExpr z3Condition = (BoolExpr) z3t.translateCondition(
                condition, getVarType(sMethodPath, ifStmt), sMethodPath);
        BoolExpr assertion = node.getType() == SType.BRANCH_TRUE ? z3Condition : ctx.mkNot(z3Condition);
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

//            if (wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR
//                    && method.getPropagationType() != MethodPropagationType.PROPAGATE) {
//                String name = node.getUnit().toString();
//                List<Expr> params = translateExpressions(method, sMethodPath);
//                Method javaMethod = (Method) SootInterpreter.getMethod(method.getInvokeExpr());
//                Expr reference = ctx.getMethodMockInstance().constructor(
//                        javaMethod, params, null, null).getReferenceExpr();
//                SVar var = new SVar(name, reference,
//                        VarType.METHOD_MOCK, null, true);
//                sMethodPath.getSymbolicVarStack().add(var);
//                return SType.INVOKE_MOCK;
//            }

//            // handle mocks
            if (wrapped.getSType() == SType.INVOKE_MOCK) {
                String name = node.getUnit().toString();
                List<Expr> params = translateExpressions(method, sMethodPath);
                Method javaMethod = (Method) SootInterpreter.getMethod(method.getInvokeExpr());
                Expr reference = ctx.getMethodMockInstance().constructor(
                        javaMethod, params, null, null).getReferenceExpr();
                SVar var = new SVar(name, reference,
                        VarType.METHOD_MOCK, null, true);
                sMethodPath.addMethodMock(var);
                return SType.INVOKE_MOCK;
            }

            // set all values to default
            if (wrapped.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
                ClassType classType = method.getInvokeExpr().getMethodSignature().getDeclClassType();
                Class<?> type = SootInterpreter.translateType(classType);
                Expr leftOpExpr = z3t.translateValue(method.getBase(), varType, sMethodPath);
                Expr expr = ctx.getClassInstance().constructor(
                        leftOpExpr, SootInterpreter.getClass(classType)).getExpr();
                ctx.getClassInstance().initialize(expr, type);
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
            } else if (methodExpr.getPropagationType() == MethodPropagationType.MODELLED) {
                Class<?> type = SootInterpreter.translateType(rightOp.getType());
                Expr expr = z3t.callProverMethod(rightOpHolder.asMethod(), rightOpVarType, sMethodPath);
                // TODO: handle complex nested objects within parameters
//                expr = ctx.mkDefault(expr, Z3Translator.translateType(rightOp.getType()));
                z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, sMethodPath, type);
            }
        } else if (rightOpHolder.getSType() == SType.INVOKE_SPECIAL_CONSTRUCTOR) {
            Class<?> type = SootInterpreter.translateType(rightOp.getType());
            Expr leftOpExpr = z3t.translateValue(leftOp, leftOpVarType, sMethodPath);
            Expr expr = ctx.getClassInstance().constructor(
                    leftOpExpr, type).getExpr();
            ctx.getClassInstance().initialize(expr, type);
            z3t.updateSymbolicVar(leftOp, expr, leftOpVarType, sMethodPath, type);
        } else if (rightOpHolder.getSType() == SType.INVOKE_MOCK) {
            // if method cannot be invoked, then mock it.
            SMethodExpr methodExpr = rightOpHolder.asMethod();
            Class rightClassType = SootInterpreter.translateType(rightOp.getType());
            List<Expr> params = translateExpressions(methodExpr, sMethodPath);
            Method method = (Method) SootInterpreter.getMethod(methodExpr.getInvokeExpr());
            Expr retValExpr = z3t.translateValue(rightOp, rightOpVarType, sMethodPath);
            Expr reference = ctx.getMethodMockInstance().constructor(
                    method, params, null, retValExpr).getReferenceExpr();
            SVar mockVar = new SVar(z3t.getValueName(leftOp), reference,
                    VarType.METHOD_MOCK, rightClassType, true);
            sMethodPath.addMethodMock(mockVar);
            return SType.INVOKE_MOCK;
        } else if (rightOp instanceof JCastExpr castExpr) {
            Value op = castExpr.getOp();
            Class classType = sMethodPath.getSymbolicVarStack().get(z3t.getValueName(op)).
                    orElseThrow().getClassType();
            z3t.updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, sMethodPath, classType);
        } else {
            Class classType = SootInterpreter.translateType(rightOp.getType());
            if (classType == Object.class) classType = SootInterpreter.translateType(leftOp.getType());
            z3t.updateSymbolicVar(leftOp, rightOpHolder.getExpr(), leftOpVarType, sMethodPath, classType);
        }
        return SType.ASSIGNMENT;
    }

    private SType handleGoto(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        List<SNode> nodes = methodPath.getTargetNodes(node.getUnit());

        // under-approximate
        for (SNode n : nodes) {
            if (depth <= CLIOptions.depthLimit) {
                analyzePaths(methodPath, n);
            }
//            if (methodPath.getClassInstance().incrementGotoCount(node)) {
//            }
        }

        return SType.GOTO;
    }

    private SType handleThrows(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        Class<?> exceptionType = sMethodPath.getSymbolicVarStack()
                .get(z3t.getValueName(((JThrowStmt) node.getUnit()).getOp())).orElseThrow()
                .getClassType();
        Optional<HandlerNode> handlerNode = sMethodPath.findHandlerNode(node, exceptionType);
        if (handlerNode.isPresent()) {
            push(sMethodPath);
            List<SNode> children = handlerNode.get().getNode().getChildren();
            assert children.size() == 1;
            children.get(0).print(1);
            analyzePaths(handlerNode.get().getMethodPath(), children.get(0));
            pop(sMethodPath);
            return SType.THROW;
        } else {
            return SType.THROW_END;
        }
    }

    private void mockThrowsAndPropagate(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        if (CLIOptions.disableMockExploration)
            return;

        String refName;
        if (node.getUnit() instanceof JAssignStmt assignStmt)
            refName = z3t.getValueName(assignStmt.getLeftOp());
        else
            refName = node.getUnit().toString();

        Expr mockReferenceExpr = sMethodPath.getSymbolicVarStack().get(refName)
                .orElseGet(() -> sMethodPath.getMethodMockStack().get(refName)
                        .orElseThrow()).getExpr();

        List<HandlerNode> handlerNodes = sMethodPath.getHandlerNodes(node);
        for (HandlerNode handlerNode : handlerNodes) {
            push(sMethodPath);
            ctx.getMethodMockInstance().setExceptionType(
                    mockReferenceExpr, handlerNode.getNode().getExceptionType());
            analyzePaths(handlerNode.getMethodPath(), handlerNode.getNode());
            pop(sMethodPath);
        }

        // clear throws so that further paths assume a return value (if non-void)
        ctx.getMethodMockInstance().setExceptionType(mockReferenceExpr, null);
    }

    private SType handleReturn(SMethodPath sMethodPath, SNode node)
            throws ClassNotFoundException {
        if (sMethodPath.getJumpNode() == null)
            return node.getType();

        push(sMethodPath);

        JumpNode jn = sMethodPath.getJumpNode();
        if (node.getType() == SType.RETURN
                && jn.getNode().getUnit() instanceof JAssignStmt assignStmt) {
            JReturnStmt stmt = (JReturnStmt) node.getUnit();
            Expr expr = z3t.translateValue(
                    stmt.getOp(), assignStmt.getRightOp().getType(), VarType.RETURN_VALUE, sMethodPath);

            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();
            VarType varType = getVarType(sMethodPath, rightOp);

            Class classType = SootInterpreter.translateType(assignStmt.getRightOp().getType());
            z3t.updateSymbolicVar(leftOp, expr, varType, jn.getMethodPath(), classType);
        }

        List<SNode> children = jn.getNode().getChildren();
        for (SNode child : children)
            analyzePaths(jn.getMethodPath(), child);

        pop(sMethodPath);

        return SType.INVOKE;
    }

    private void propagate(SMethodExpr methodExpr, SMethodPath methodPath, JumpNode jumpNode)
            throws ClassNotFoundException {
        // under-approximate
        if (depth <= CLIOptions.depthLimit) {
            SParamList paramList = createParamList(methodExpr, methodPath);
            Executable method = SootInterpreter.getMethod(methodExpr.getInvokeExpr());
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
                .map(e -> SootInterpreter.translateType(e.getType()))
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
        Local ref = ((JIdentityStmt) unit).getLeftOp();
        Type type = ((JIdentityStmt) unit).getRightOp().getType();
        SVar param = z3t.saveSymbolicVar(
                ref,
                type,
                getVarType(methodPath, unit),
                methodPath);

        if (SortType.MAP.equals(param.getExpr().getSort()))
            ctx.getMapInstance().parameterConstructor(param.getExpr());
        else if (SortType.ARRAY.equals(param.getExpr().getSort()))
            ctx.getLinkedListInstance().parameterConstructor(param.getExpr());
        else if (SortType.OBJECT.equals(param.getExpr().getSort())
                && CLIOptions.shouldPropagate(param.getClassType().getName()))
            ctx.getClassInstance().parameterConstructor(param.getExpr(), param.getClassType());
    }

    private void updateParameter(SMethodPath methodPath, Stmt unit) {
        Local ref = ((JIdentityStmt) unit).getLeftOp();
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
        sMethodPath.push();
        z3t.getStack().push();
        depth++;
    }

    private void pop(SMethodPath sMethodPath) {
        solver.pop();
        sMethodPath.pop();
        z3t.getStack().pop();
        depth--;
    }

    public static void main(String[] args) throws ClassNotFoundException {
        new Z3Translator();

        Z3ExtendedContext ctx = Z3Translator.getContext();
        Z3ExtendedSolver solver = ctx.getSolver();

        ArrayExpr set1 = ctx.mkEmptySet(ctx.mkIntSort());
        ArrayExpr set2 = ctx.mkEmptySet(ctx.mkIntSort());

        set1 = ctx.mkSetAdd(set1, ctx.mkInt(1));
        set1 = ctx.mkSetAdd(set1, ctx.mkInt(3));
        set2 = ctx.mkSetAdd(set2, ctx.mkInt(3));
        set2 = ctx.mkSetAdd(set2, ctx.mkInt(1));

        FuncDecl mapper = ctx.mkFreshFuncDecl("Mapper", new Sort[]{ctx.mkSetSort(ctx.mkIntSort())}, ctx.mkIntSort());

        Expr res1 = mapper.apply(set1);
        Expr res2 = mapper.apply(set2);

        BoolExpr condition = ctx.mkEq(res1, res2);

        solver.add(ctx.mkNot(condition));
        Status status = solver.check();
        String asd = "asd";
    }
}