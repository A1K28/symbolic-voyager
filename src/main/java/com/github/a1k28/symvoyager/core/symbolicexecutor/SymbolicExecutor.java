package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.handler.SymbolicHandlerContext;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.Z3Status;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SClassInstance;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SParamList;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import com.github.a1k28.symvoyager.helper.Logger;
import com.microsoft.z3.*;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.common.stmt.Stmt;

import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import static com.github.a1k28.symvoyager.helper.OSDependentZ3Loader.loadZ3Library;

public class SymbolicExecutor {
    private static final Logger log = Logger.getInstance(SymbolicExecutor.class);

    private final Z3Translator z3t;

    private Z3ExtendedSolver solver;
    private Z3ExtendedContext ctx;
    private SymbolicHandlerContext symbolicHandlerContext;
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

    public void analyzePaths(SMethodPath methodPath, BasicBlock<?> block) throws ClassNotFoundException {
        analyzePaths(methodPath, block,-1);
    }

    public void analyzePaths(SMethodPath methodPath, BasicBlock<?> block, int start) throws ClassNotFoundException {
        if (!satHandler.isSatisfiable(methodPath)) return;  // check
        if (depth > CLIOptions.depthLimit) return;  // under-approximate

        Stmt stmt = null;
        SType type = SType.OTHER;
        for (int i = start+1; i<block.getStmts().size(); i++) {
            stmt = block.getStmts().get(i);
            type = symbolicHandlerContext.handle(methodPath, stmt);

            if (shouldEndPropagation(type)) return;
        }

        Z3Status status = satHandler.checkSatisfiability(methodPath, stmt, type);
        if (Z3Status.SATISFIABLE != status) return;
        for (BasicBlock<?> successor : block.getSuccessors()) {
            push(methodPath);
            analyzePaths(methodPath, successor);
            pop(methodPath);
        }
    }

    private boolean shouldEndPropagation(SType type) {
        return SType.INVOKE == type || SType.BRANCH == type || SType.SWITCH == type;
    }

    private void printMethod(SClassInstance classInstance, Executable method) {
        log.debug("Printing method: " + method.getName());
//        ctx.getClassInstance().getMethodPath(classInstance, method).print();
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