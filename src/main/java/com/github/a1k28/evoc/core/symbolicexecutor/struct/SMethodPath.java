package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.JumpNode;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.helper.Logger;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.model.Body;

import java.lang.reflect.Method;
import java.util.*;

@Getter
public class SMethodPath {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    private final Body body;
    private final SNode root;
    private final Method method;
    private final Map<Stmt, SNode> sNodeMap; // used for GOTO tracking
    private final Map<SNode, Integer> gotoCount; // used for tracking GOTO execution count
    private SParamList paramList;
    private SatisfiableResults satisfiableResults;
    private JumpNode jumpNode;

    public SMethodPath(Body body, Method method) {
        this.body = body;
        this.method = method;
        this.root = new SNode();
        this.sNodeMap = new HashMap<>();
        this.gotoCount = new HashMap<>();
    }

    public SMethodPath(SMethodPath skeleton, SParamList paramList, JumpNode jumpNode) {
        this.body = skeleton.body;
        this.method = skeleton.method;
        this.root = skeleton.root;
        this.sNodeMap = skeleton.sNodeMap;
        this.paramList = paramList;
        this.satisfiableResults = new SatisfiableResults(new ArrayList<>(), method);
        this.gotoCount = new HashMap<>();
        this.jumpNode = jumpNode;
    }

    public SNode createNode(Stmt unit) {
        SType type = getType(unit);
        SNode sNode = new SNode(unit, type);
        assert !sNodeMap.containsKey(unit) || sNodeMap.get(unit).getUnit() == unit;
        sNodeMap.put(unit, sNode);
        return sNode;
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

    public boolean incrementGotoCount(SNode sNode) {
        if (!this.gotoCount.containsKey(sNode))
            this.gotoCount.put(sNode, 0);
        this.gotoCount.put(sNode, this.gotoCount.get(sNode) + 1);
        return this.gotoCount.get(sNode) <= 10; // limit is 10
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
            if (val instanceof JParameterRef) {
                return SType.PARAMETER;
            }
            return SType.IDENTITY;
        }
        log.warn("Could not identify: " + unit);
        return SType.OTHER;
    }
}
