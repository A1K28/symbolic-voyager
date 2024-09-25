package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.HandlerNode;
import com.github.a1k28.evoc.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.core.sootup.SootInterpreter;
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
public class SMethodPath {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    // fixed parameters
    private final Body body;
    private final SNode root;
    private final Method method;
    private final Map<Stmt, List<SNode>> sNodeMap; // used for GOTO tracking
    private final SClassInstance classInstance;

    // dynamic parameters
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

    public SMethodPath getTopMethodPath() {
        if (jumpNode != null) {
            return jumpNode.getMethodPath().getTopMethodPath();
        }
        return this;
    }

    public Optional<HandlerNode> findHandlerNode(SNode node, Class<?> type) {
        if (node == null) {
            if (jumpNode == null) return Optional.empty();
            return jumpNode.getMethodPath().findHandlerNode(jumpNode.getNode(), type);
        }
        for (Trap trap : body.getTraps()) {
            if (!trap.getBeginStmt().equals(node.getUnit())) continue;
            Class<?> trapType = SootInterpreter.getClass(trap.getExceptionType());
            if (!trapType.isAssignableFrom(type)) continue;
            SCatchNode catchNode = (SCatchNode) sNodeMap.get(trap.getHandlerStmt()).get(0);
            return Optional.of(new HandlerNode(this, catchNode));
        }
        return findHandlerNode(node.getParent(), type);
    }

    public List<HandlerNode> getHandlerNodes(SNode node) {
        List<HandlerNode> handlerNodes = new ArrayList<>();
//        findHandlerNodes(node, handlerNodes, false);
        return handlerNodes;
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

//    private void findHandlerNodes(SNode node, List<HandlerNode> handlerNodes, boolean findAll) {
//        findHandlerNodesHelper(node, handlerNodes, findAll);
//        if (jumpNode != null && handlerNodes.isEmpty()) {
//            jumpNode.getMethodPath().findHandlerNodes(jumpNode.getNode(), handlerNodes, findAll);
//        }
//    }

//    private void findHandlerNodesHelper(SNode node, List<HandlerNode> handlerNodes, boolean findAll) {
//        if (node == null) return;
//        if (node.getCatchBlocks().isEmpty())
//            findHandlerNodesHelper(node.getParent(), handlerNodes, findAll);
//        else {
//            getTopLevelCatchBlocks(node, findAll).forEach(catchNode ->
//                    handlerNodes.add(new HandlerNode(this, catchNode)));
//        }
//    }

//    private static List<SCatchNode> getTopLevelCatchBlocks(SNode node, boolean findAll) {
//        List<SCatchNode> nodes = new ArrayList<>();
//        Map<SCatchNode, Integer> depthMap = new HashMap<>();
//        int maxDepth = 0;
//        for (SCatchNode catchNode : node.getCatchBlocks()) {
//            int depth = getExceptionDepth(catchNode);
//            maxDepth = Math.max(depth, maxDepth);
//            depthMap.put(catchNode, getExceptionDepth(catchNode));
//        }
//        if (findAll) {
//            // sort
//            List<Map.Entry<SCatchNode, Integer>> list = new ArrayList<>(depthMap.entrySet());
//            list.sort(Map.Entry.comparingByValue());
//            for (int i = list.size()-1; i >= 0; i--)
//                nodes.add(list.get(i).getKey());
//        } else {
//            int finalMaxDepth = maxDepth;
//            depthMap.entrySet().stream()
//                    .filter(e -> e.getValue() == finalMaxDepth)
//                    .forEach(e -> nodes.add(e.getKey()));
//        }
//        return nodes;
//    }

//    private static int getExceptionDepth(SNode node) {
//        if (node.getCatchBlocks().isEmpty()) return 0;
//        int max = 0;
//        for (SNode catchBlock : node.getCatchBlocks()) {
//            int res = 1 + getExceptionDepth(catchBlock);
//            max = Math.max(res, max);
//        }
//        return max;
//    }

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
}
