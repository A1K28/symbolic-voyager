package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.github.a1k28.symvoyager.core.sootup.SootInterpreter;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.HandlerNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.github.a1k28.symvoyager.helper.Logger;
import lombok.Getter;
import sootup.core.jimple.basic.Trap;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.model.Body;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.*;

@Getter
public class SMethodPath implements IStack {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    // fixed parameters
    private final Body body;
    private final SNode root;
    private final Method method;
    private final Map<Stmt, List<SNode>> sNodeMap; // used for GOTO tracking
    private final Map<Stmt, List<Stmt>> handlerMap;
    private final SClassInstance classInstance;

    // dynamic parameters
    private SParamList paramList;
    private SatisfiableResults satisfiableResults;
    private JumpNode jumpNode;
    private SStack symbolicVarStack;
    private SStack methodMockStack;

    public SMethodPath(SClassInstance classInstance, Body body, Method method) {
        this.classInstance = classInstance;
        this.body = body;
        this.method = method;
        this.root = new SNode();
        this.sNodeMap = new HashMap<>();
        this.handlerMap = new HashMap<>();
    }

    public SMethodPath(SMethodPath skeleton,
                       SParamList paramList,
                       JumpNode jumpNode) {
        this.classInstance = skeleton.classInstance;
        this.body = skeleton.body;
        this.method = skeleton.method;
        this.root = skeleton.root;
        this.sNodeMap = skeleton.sNodeMap;
        this.handlerMap = skeleton.handlerMap;
        this.methodMockStack = skeleton.methodMockStack;
        this.paramList = paramList;
        this.satisfiableResults = new SatisfiableResults(new ArrayList<>(), method);
        this.jumpNode = jumpNode;
        this.symbolicVarStack = new SStack();
        this.methodMockStack = new SStack();
    }

    public SNode getNode(Stmt unit) {
        if (sNodeMap.containsKey(unit))
            return sNodeMap.get(unit).get(0);
        return createNode(unit);
    }

    public SNode getNode(Stmt unit, SType type) {
        return getNodeOptional(unit, type).orElseGet(() -> createNode(unit, type));
    }

    public Optional<SNode> getNodeOptional(Stmt unit, SType type) {
        if (sNodeMap.containsKey(unit)) {
            for (SNode node : sNodeMap.get(unit)) {
                if (node.getType() == type)
                    return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public SNode createNode(Stmt unit) {
        SType type = getType(unit);
        return createNode(unit, type);
    }

    public SNode createNode(Stmt unit, SType type) {
        SNode sNode = new SNode(unit, type);
        if (!sNodeMap.containsKey(unit))
            sNodeMap.put(unit, new ArrayList<>());
        sNodeMap.get(unit).add(sNode);
        return sNode;
    }

    public SCatchNode createCatchNode(Stmt unit, ClassType exceptionType) {
        SCatchNode sNode = new SCatchNode(unit, exceptionType);
        if (!sNodeMap.containsKey(unit))
            sNodeMap.put(unit, new ArrayList<>());
        sNodeMap.get(unit).add(sNode);
        return sNode;
    }

    public SStack getMethodMockStack() {
        return getTopMethodPath().methodMockStack;
    }

    public List<SVar> getAllSymbolicVars() {
        List<SVar> vars = symbolicVarStack.getAll();
        vars.addAll(classInstance.getSymbolicFieldStack().getAll());
        vars.addAll(getTopMethodPath().methodMockStack.getAll());
        return vars;
    }

    public void addMethodMock(SVar var) {
        this.symbolicVarStack.add(var);
        this.getTopMethodPath().methodMockStack.add(var);
    }

    public Optional<HandlerNode> findHandlerNode(SNode node, Class<?> type) {
        if (node == null) {
            if (jumpNode == null)
                return Optional.empty();
            return jumpNode.getMethodPath().findHandlerNode(jumpNode.getNode(), type);
        }
        if (handlerMap.containsKey(node.getUnit())) {
            for (Stmt handlerStmt : handlerMap.get(node.getUnit())) {
                Trap trap = null;
                for (Trap t : body.getTraps()) {
                    if (t.getHandlerStmt().equals(handlerStmt)) {
                        trap = t;
                        break;
                    }
                }
                assert trap != null;
                Class<?> trapType = SootInterpreter.getClass(trap.getExceptionType());
                if (!trapType.isAssignableFrom(type)) continue;
                SCatchNode catchNode = (SCatchNode) sNodeMap.get(trap.getHandlerStmt()).get(0);
                return Optional.of(new HandlerNode(this, catchNode));
            }
        }
        return findHandlerNode(node.getParent(), type);
    }

    public List<HandlerNode> getHandlerNodes(SNode node) {
        List<HandlerNode> handlerNodes = new ArrayList<>();
        findHandlerNodes(node, handlerNodes);

        // filter out unreachable catch blocks
        List<HandlerNode> filteredHandlerNodes = new ArrayList<>();
        outer: for (int i = handlerNodes.size() - 1; i >= 0; i--) {
            HandlerNode hn1 = handlerNodes.get(i);
            Class<?> type1 = SootInterpreter.getClass(hn1.getNode().getExceptionType());
            for (int k = i-1; k >= 0; k--) {
                HandlerNode hn2 = handlerNodes.get(k);
                Class<?> type2 = SootInterpreter.getClass(hn2.getNode().getExceptionType());
                if (type2.isAssignableFrom(type1)) continue outer;
            }
            filteredHandlerNodes.add(hn1);
        }

        Collections.reverse(filteredHandlerNodes);
        return filteredHandlerNodes;
    }

    private void findHandlerNodes(SNode node, List<HandlerNode> handlerNodes) {
        if (node == null) {
            if (jumpNode == null) return;
            jumpNode.getMethodPath().findHandlerNodes(jumpNode.getNode(), handlerNodes);
        } else {
            if (handlerMap.containsKey(node.getUnit())) {
                for (Stmt handlerStmt : handlerMap.get(node.getUnit())) {
                    Trap trap = null;
                    for (Trap t : body.getTraps()) {
                        if (t.getHandlerStmt().equals(handlerStmt)) {
                            trap = t;
                            break;
                        }
                    }
                    assert trap != null;
                    SCatchNode catchNode = (SCatchNode) sNodeMap.get(trap.getHandlerStmt()).get(0);
                    handlerNodes.add(new HandlerNode(this, catchNode));
                }
            }
            findHandlerNodes(node.getParent(), handlerNodes);
        }
    }

    private SMethodPath getTopMethodPath() {
        if (jumpNode != null)
            return jumpNode.getMethodPath().getTopMethodPath();
        return this;
    }

    public void print() {
        this.root.print(2);
        log.empty();
    }

    public List<SNode> getTargetNodes(Stmt unit) {
        List<SNode> sNodes = new ArrayList<>();
        for (Stmt target : ((JGotoStmt)unit).getTargetStmts(body)) {
            for (Stmt child : body.getStmtGraph().getBlockOf(target).getStmts()) {
                assert sNodeMap.containsKey(child);
                sNodes.addAll(sNodeMap.get(child));
            }
        }
        return sNodes;
    }

    private static SType getType(Stmt unit) {
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
        if (methodMockStack != null)
            methodMockStack.push();
    }

    @Override
    public void pop() {
        symbolicVarStack.pop();
        classInstance.getSymbolicFieldStack().pop();
        if (methodMockStack != null)
            methodMockStack.pop();
    }
}
