package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.*;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SClassInstance;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SParamList;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import com.github.a1k28.symvoyager.helper.Logger;
import com.microsoft.z3.*;
import lombok.Getter;
import sootup.core.jimple.common.stmt.JAssignStmt;

import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

import static com.github.a1k28.symvoyager.helper.OSDependentZ3Loader.loadZ3Library;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);

    private final Z3Translator z3t;

    private Z3ExtendedSolver solver;
    private Z3ExtendedContext ctx;
    private SymbolicHandlerContext symbolicHandlerContext;
    private final SatisfiabilityHandler satHandler;

    @Getter
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
        this.symbolicHandlerContext = new SymbolicHandlerContext(this, z3t, ctx);
    }

    public void refresh() {
        z3t.initZ3(true);
        this.ctx = z3t.getContext();
        this.solver = ctx.getSolver();
        this.satHandler.refresh();
        this.symbolicHandlerContext = new SymbolicHandlerContext(this, z3t, ctx);
    }

    public void push(SMethodPath sMethodPath) {
        solver.push();
        sMethodPath.push();
        z3t.getStack().push();
        depth++;
    }

    public void pop(SMethodPath sMethodPath) {
        solver.pop();
        sMethodPath.pop();
        z3t.getStack().pop();
        depth--;
    }

    public void addAssertion(BoolExpr expr) {
        this.solver.add(expr);
    }

    public SClassInstance getClassInstance(Class<?> clazz) throws ClassNotFoundException {
        return ctx.getClassInstance().constructor(clazz).getClassInstance();
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

    public void analyzePaths(SMethodPath sMethodPath, SNode node) throws ClassNotFoundException {
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE){
            push(sMethodPath);
        }

        SType type = symbolicHandlerContext.handle(sMethodPath, node);

        // expand paths by allowing method mocks to throw exceptions
        if (type == SType.INVOKE_MOCK) {
            mockThrowsAndPropagate(sMethodPath, node);
        }

        Z3Status status = satHandler.checkSatisfiability(sMethodPath, node, type);
        if (Z3Status.SATISFIABLE == status)
            for (SNode child : node.getChildren())
                analyzePaths(sMethodPath, child);

        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            pop(sMethodPath);
        }
    }

    private void printMethod(SClassInstance classInstance, Executable method) {
        log.debug("Printing method: " + method.getName());
        ctx.getClassInstance().getMethodPath(classInstance, method).print();
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