package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.HandlerNode;
import com.github.a1k28.evoc.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
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
import java.util.stream.Collectors;

@Getter
public class SMethodPath {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    private final Body body;
    private final SNode root;
    private final Method method;
    private final Map<Stmt, SNode> sNodeMap; // used for GOTO tracking
    private final SClassInstance classInstance;
    private SParamList paramList;
    private SatisfiableResults satisfiableResults;
    private JumpNode jumpNode;
    private SStack symbolicVarStack;

    public SMethodPath(SClassInstance classInstance, Body body, Method method) {
        this.classInstance = classInstance;
        this.body = body;
        this.method = method;
        this.root = new SNode();
        this.sNodeMap = new HashMap<>();
    }

    public SMethodPath(SMethodPath skeleton,
                       SParamList paramList,
                       JumpNode jumpNode,
                       SStack symbolicVarStack) {
        this.classInstance = skeleton.classInstance;
        this.body = skeleton.body;
        this.method = skeleton.method;
        this.root = skeleton.root;
        this.sNodeMap = skeleton.sNodeMap;
        this.paramList = paramList;
        this.satisfiableResults = new SatisfiableResults(new ArrayList<>(), method);
        this.jumpNode = jumpNode;
        this.symbolicVarStack = symbolicVarStack;
    }

    public SNode createNode(Stmt unit) {
        SType type = getType(unit);
        SNode sNode = new SNode(unit, type);
        assert !sNodeMap.containsKey(unit) || sNodeMap.get(unit).getUnit() == unit;
        sNodeMap.put(unit, sNode);
        return sNode;
    }

    public HandlerNode getHandlerNode(SNode node, Class<?> exception) {
        if (node == null)
            return jumpNode == null ?
                    null : jumpNode.getMethodPath().getHandlerNode(jumpNode.getNode(), exception);

        for (SNode catchBlock : node.getCatchBlocks()) {
            Trap handlerTrap = getHandlerTrap(catchBlock, exception);
            if (handlerTrap != null)
                return new HandlerNode(this, catchBlock, handlerTrap);
        }

        return getHandlerNode(node.getParent(), exception);
    }

    public List<HandlerNode> getHandlerNodes(SNode node) {
        List<HandlerNode> handlerNodes = new ArrayList<>();
        getHandlerNodesHelper(node, handlerNodes);
        return handlerNodes;
    }

    public void print() {
        this.root.print(2);
        log.empty();
    }

    public List<SNode> getSNodes(Stmt unit) {
        List<SNode> sNodes = new ArrayList<>();
        for (Stmt target : ((JGotoStmt)unit).getTargetStmts(body)) {
            assert sNodeMap.containsKey(target);
            sNodes.add(sNodeMap.get(target));
        }
        return sNodes;
    }

    private void getHandlerNodesHelper(SNode node, List<HandlerNode> handlerNodes) {
        for (SNode catchNode : node.getCatchBlocks()) {
            HandlerNode handlerNode = new HandlerNode(this, catchNode, getTrap(catchNode));
            handlerNodes.add(handlerNode);
        }

        // TODO: reconsider this
//        if (jumpNode != null) {
//            jumpNode.getMethodPath().getHandlerNodesHelper(jumpNode.getNode(), handlerNodes);
//        }
    }

    private Trap getTrap(SNode node) {
        for (Trap trap : body.getTraps()) {
            if (trap.getHandlerStmt().equals(node.getUnit()))
                return trap;
        }
        throw new IllegalStateException("Trap not found");
    }

    private Trap getHandlerTrap(SNode node, Class<?> exception) {
        for (Trap trap : body.getTraps()) {
            if (trap.getHandlerStmt().equals(node.getUnit())) {
                Class<?> trapClass = SootHelper.getClass(trap.getExceptionType());
                if (trapClass.isAssignableFrom(exception))
                    return trap;
            }
        }
        return null;
    }

    private static SType getType(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JRetStmt.class) return SType.RETURN;
        if (clazz == JGotoStmt.class)
            return SType.GOTO;
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
}
