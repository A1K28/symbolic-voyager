package com.github.a1k28.evoc.core.symbex.struct;

import lombok.Getter;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.ParameterRef;
import soot.jimple.internal.JIdentityStmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SPath {
    private final SNode root;
    private final Map<String, ParameterRef> nameToParamIdx;

    public SPath() {
        this.root = new SNode();
        this.nameToParamIdx = new HashMap<>();
    }

    public SNode createNode(Unit unit) {
        return new SNode(unit, getType(unit));
    }

    public void print() {
        this.root.print(1);
    }

    // returns an empty path if unsatisfiable (pruning)
    public List<SNode> getNextPath() {
        List<SNode> path = new ArrayList<>();
        for (SNode child : root.getChildren())
            getNextPathHelper(child, path);
        return path;
    }

    private void getNextPathHelper(SNode current, List<SNode> list) {
        if (!current.isSatisfiable()) {
            list.clear();
            current.setVisited(true);
            return;
        }

        if (current.isVisited()) return;
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

    private SType getType(Unit unit) {
        if (unit instanceof IfStmt) return SType.BRANCH;
        if (unit instanceof AssignStmt) return SType.ASSIGNMENT;
        if (unit instanceof JIdentityStmt u) {
            Value val = u.getRightOpBox().getValue();
            if (val instanceof ParameterRef v) {
                this.nameToParamIdx.put(u.getLeftOpBox().getValue().toString(), v);
                return SType.PARAMETER;
            }
        }
        return SType.OTHER;
    }
}
