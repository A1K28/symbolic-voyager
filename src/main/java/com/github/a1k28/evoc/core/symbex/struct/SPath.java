package com.github.a1k28.evoc.core.symbex.struct;

import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.JBreakpointStmt;
import sootup.core.jimple.javabytecode.stmt.JRetStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SPath {
    private final SNode root;
    private final Map<String, JParameterRef> nameToParamIdx;

    public SPath() {
        this.root = new SNode();
        this.nameToParamIdx = new HashMap<>();
    }

    public SNode createNode(Stmt unit) {
        return new SNode(unit, getType(unit));
    }

    public void print() {
        this.root.print(1);
    }

    // returns an empty path if unsatisfiable (pruning)
    public List<SNode> getNextPath() {
        if (root.isVisited()) return null;
        List<SNode> path = new ArrayList<>();
        getNextPathHelper(root, path);
        return path;
    }

    private void getNextPathHelper(SNode current, List<SNode> list) {
        if (!current.isSatisfiable()) {
            list.clear();
            current.setVisited(true);
            return;
        }

        if (current.isVisited()) return;

        if (current.getType() != SType.ROOT)
            list.add(current);

        if (current.getChildren().isEmpty())
            current.setVisited(true);

        for (SNode ch : current.getChildren()) {
            if (ch.isVisited()) continue;
            getNextPathHelper(ch, list);
            break;
        }

        boolean allChildrenVisited = true;
        for (SNode ch : current.getChildren())
            allChildrenVisited = allChildrenVisited && ch.isVisited();
        if (allChildrenVisited)
            current.setVisited(true);
    }

    private SType getType(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JAssignStmt.class) return SType.ASSIGNMENT;
        if (clazz == JGotoStmt.class) return SType.GOTO;
        if (clazz == JRetStmt.class) return SType.RETURN;
        if (clazz == JReturnStmt.class) return SType.RETURN;
        if (clazz == JInvokeStmt.class) return SType.INVOKE;
        if (clazz == JSwitchStmt.class) return SType.SWITCH;
        if (clazz == JThrowStmt.class) return SType.THROW;
        if (clazz == JReturnVoidStmt.class) return SType.RETURN_VOID;
        if (clazz == JNopStmt.class) return SType.NOP;
        if (clazz == JBreakpointStmt.class) return SType.BREAKPOINT;
        if (unit instanceof JIdentityStmt u) {
            Value val = u.getRightOp();
            if (val instanceof JParameterRef v) {
                this.nameToParamIdx.put(u.getLeftOp().toString(), v);
                return SType.PARAMETER;
            }
        }
        return SType.OTHER;
    }
}