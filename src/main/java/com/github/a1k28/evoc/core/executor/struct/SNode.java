package com.github.a1k28.evoc.core.executor.struct;

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

    public SNode() {
        this.unit = null;
        this.children = new ArrayList<>();
        this.type = SType.ROOT;
    }

    public SNode(Stmt unit, SType sType) {
        this.unit = unit;
        this.type = sType;
        this.children = new ArrayList<>();
    }

    public void addChild(SNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    public boolean containsParent(Stmt unit) {
        if (this.unit == null) return false;
        if (this.unit.equals(unit)) return true;
        if (this.parent != null) return this.parent.containsParent(unit);
        return false;
    }

    public void print(int level) {
        for (int i = 1; i < level; i++) System.out.print("\t");
        System.out.println(this);
        for (SNode child : getChildren()) child.print(level + 1);
    }

    @Override
    public String toString() {
        return this.type + " " + this.unit;
    }
}
