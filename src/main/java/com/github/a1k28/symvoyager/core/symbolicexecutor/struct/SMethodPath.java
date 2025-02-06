package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.*;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.github.a1k28.symvoyager.helper.Logger;
import lombok.Getter;
import sootup.core.graph.BasicBlock;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.model.Body;

import java.lang.reflect.Method;
import java.util.*;

@Getter
public class SMethodPath implements IStack {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    // fixed parameters
    private final Body body;
    private final BasicBlock<?> root;
    private final Method method;
    private final Map<Stmt, List<Stmt>> handlerMap;
    private final SClassInstance classInstance;
    private final Map<Stmt, StmtCache> stmtCacheMap;

    // dynamic parameters
    private SParamList paramList;
    private SatisfiableResults satisfiableResults;
    private JumpNode jumpNode;
    private SStack symbolicVarStack;
    private SStack methodMockStack;

    public SMethodPath(SClassInstance classInstance, Body body, BasicBlock<?> root, Method method) {
        this.classInstance = classInstance;
        this.body = body;
        this.method = method;
        this.root = root;
        this.handlerMap = new HashMap<>();
        this.stmtCacheMap = new HashMap<>();
        initStmtCache(root);
    }

    public SMethodPath(SMethodPath skeleton,
                       SParamList paramList,
                       JumpNode jumpNode) {
        this.classInstance = skeleton.classInstance;
        this.body = skeleton.body;
        this.method = skeleton.method;
        this.root = skeleton.root;
        this.handlerMap = skeleton.handlerMap;
        this.methodMockStack = skeleton.methodMockStack;
        this.stmtCacheMap = skeleton.stmtCacheMap;
        this.paramList = paramList;
        this.satisfiableResults = new SatisfiableResults(new ArrayList<>(), method);
        this.jumpNode = jumpNode;
        this.symbolicVarStack = new SStack();
        this.methodMockStack = new SStack();
    }

    public BasicBlock<?> getBlock(Stmt stmt) {
        return stmtCacheMap.get(stmt).getBlock();
    }

    public SType getType(Stmt stmt) {
        return stmtCacheMap.get(stmt).getType();
    }

    public JumpNode createJumpNode(Stmt stmt) {
        BasicBlock<?> block = stmtCacheMap.get(stmt).getBlock();
        int i = 0;
        for (;i<block.getStmts().size() && stmt != block.getStmts().get(i);i++) {}
        return new JumpNode(this, block, i);
    }

    public SStack getMethodMockStack() {
        return getTopMethodPath().methodMockStack;
    }

    public List<SVar> getAllSymbolicVars() {
        SMethodPath sMethodPath = getTopMethodPath();
        List<SVar> vars = sMethodPath.symbolicVarStack.getAll();
        vars.addAll(sMethodPath.classInstance.getSymbolicFieldStack().getAll());
        vars.addAll(sMethodPath.methodMockStack.getAll());
        return vars;
    }

    public void addMethodMock(SVar var) {
        this.symbolicVarStack.add(var);
        this.getTopMethodPath().methodMockStack.add(var);
    }

    public void addSatisfiableResult(SatisfiableResult result) {
        getTopMethodPath().getSatisfiableResults().getResults().add(result);
    }

    private SMethodPath getTopMethodPath() {
        if (jumpNode != null)
            return jumpNode.getMethodPath().getTopMethodPath();
        return this;
    }

    public void print() {
        this.print(root, 1);
        log.empty();
    }

    private void print(BasicBlock<?> block, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) sb.append("\t");
        for (Stmt stmt : block.getStmts()) System.out.println(sb.toString() + stmt);
        for (BasicBlock<?> successor : block.getSuccessors()) {
            print(successor, level+1);
            System.out.println(sb+"----------------------");
        }
    }

    // TODO: maybe take this outside of scope?
    private final Set<BasicBlock<?>> visited = new HashSet<>();
    private void initStmtCache(BasicBlock<?> block) {
        if (visited.contains(block)) return;
        visited.add(block);
        for (Stmt stmt : block.getStmts()) {
            if (stmtCacheMap.containsKey(stmt)) continue;
            StmtCache cache = new StmtCache(getTypeInternal(stmt), block);
            stmtCacheMap.put(stmt, cache);
        }
        for (BasicBlock<?> successor : block.getSuccessors())
            initStmtCache(successor);
        for (BasicBlock<?> successor : block.getExceptionalSuccessors().values())
            initStmtCache(successor);
    }

    private static SType getTypeInternal(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JRetStmt.class) return SType.RETURN;
        if (clazz == JGotoStmt.class) return SType.GOTO;
        if (clazz == JAssignStmt.class) return SType.ASSIGNMENT;
        if (clazz == JReturnStmt.class) return SType.RETURN;
        if (clazz == JInvokeStmt.class) return SType.INVOKE;
        if (clazz == JSwitchStmt.class) return SType.SWITCH;
        if (clazz == JThrowStmt.class) return SType.THROW;
        if (clazz == JEnterMonitorStmt.class) return SType.ENTER_MONITOR;
        if (clazz == JExitMonitorStmt.class) return SType.EXIT_MONITOR;
        if (clazz == JReturnVoidStmt.class) return SType.RETURN_VOID;
        if (clazz == JNopStmt.class) return SType.NOP;
        if (clazz == JBreakpointStmt.class) return SType.BREAKPOINT;
        if (unit instanceof JIdentityStmt u) {
            Value val = u.getRightOp();
            if (val instanceof JParameterRef) return SType.PARAMETER;
            if (val instanceof JCaughtExceptionRef) return SType.CATCH;
            return SType.IDENTITY;
        }
        log.warn("Could not identify: " + unit);
        return SType.OTHER;
    }

    @Override
    public void push() {
        symbolicVarStack.push();
        classInstance.getSymbolicFieldStack().push();
        getTopMethodPath().getMethodMockStack().push();
    }

    @Override
    public void pop() {
        symbolicVarStack.pop();
        classInstance.getSymbolicFieldStack().pop();
        getTopMethodPath().getMethodMockStack().pop();
    }
}
