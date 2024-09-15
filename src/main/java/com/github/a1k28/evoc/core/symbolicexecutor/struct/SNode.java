package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import lombok.Getter;
import lombok.Setter;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SNode {
    private final Stmt unit;
    private SType type;
    private SNode parent;
    private final List<SNode> children;
    private final List<SNode> catchBlocks;

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
            this.catchBlocks.add(child);
        } else {
            this.children.add(child);
            child.setParent(this);
        }
    }

    public void removeLastChild() {
        if (this.children.isEmpty()) return;
        this.children.get(this.children.size()-1).setParent(this);
        this.children.remove(this.children.size()-1);
    }

    public boolean containsParent(Stmt unit) {
        if (this.unit == null) return false;
        if (this.unit.equals(unit)) return true;
        if (this.parent != null) return this.parent.containsParent(unit);
        return false;
    }

    public Stmt getUnit() {
        return this.unit;
    }

    public void print(int level) {
        for (int i = 1; i < level; i++) System.out.print("\t");
        if (this.getType() == SType.BRANCH_FALSE
                || this.getType() == SType.BRANCH_TRUE
                || this.getType() == SType.SWITCH
                || this.getType() == SType.GOTO)
            level++;
        System.out.println(this);
        for (SNode child : getChildren())
            child.print(level);
    }

    @Override
    public String toString() {
        return this.type + " " + unit;
    }
}
