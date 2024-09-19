package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import lombok.Getter;
import lombok.Setter;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SNode {
    protected final Stmt unit;
    protected SType type;
    private SNode parent;
    private final List<SNode> children;
    private final List<SCatchNode> catchBlocks;

    public SNode() {
        this.unit = null;
        this.children = new ArrayList<>();
        this.catchBlocks = new ArrayList<>();
        this.type = SType.ROOT;
    }

    public SNode(Stmt unit, SType sType) {
        this.unit = unit;
        this.type = sType;
        this.children = new ArrayList<>();
        this.catchBlocks = new ArrayList<>();
    }

    public void addChild(SNode child) {
        if (child.getType() == SType.CATCH) {
            if (!this.catchBlocks.contains(child))
                this.catchBlocks.add((SCatchNode) child);
            if (this.getType() == SType.CATCH)
                child.setParent(this);
        } else {
            if (!this.children.contains(child))
                this.children.add(child);
            child.setParent(this);
        }
    }

    public void removeLastChild() {
        if (this.children.isEmpty()) return;
        this.children.get(this.children.size()-1).setParent(null); // TODO: ??
        this.children.remove(this.children.size()-1);
    }

    public boolean containsParent(Stmt unit) {
        return this.containsParent(unit, null);
    }

    public Stmt getUnit() {
        return this.unit;
    }

    public void print(int level) {
        for (int i = 1; i < level; i++) System.out.print("\t");
        if (this.getType() == SType.BRANCH_FALSE
                || this.getType() == SType.BRANCH_TRUE
                || this.getType() == SType.SWITCH
                || this.getType() == SType.CATCH
                || this.getType() == SType.GOTO)
            level++;
        System.out.println(this);
        for (SNode child : getChildren())
            child.print(level);
        for (SNode child : getCatchBlocks())
            child.print(level);
    }

    @Override
    public String toString() {
        return this.type + " " + unit;
    }

    private boolean containsParent(Stmt unit, Stmt original) {
        if (this.unit != null && this.unit.equals(unit)) return true;
        if (original != null && this.unit == original) return false; // terminate recursion
        for (Stmt catchStmt : this.catchBlocks.stream().map(SNode::getUnit).toList()) {
            if (catchStmt.equals(unit)) return true;
        }
        if (original == null) original = this.unit;
        if (this.parent != null) return this.parent.containsParent(unit, original);
        return false;
    }
}
