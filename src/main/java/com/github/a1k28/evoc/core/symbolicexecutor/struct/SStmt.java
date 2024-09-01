package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import lombok.Getter;
import sootup.core.jimple.common.stmt.Stmt;

@Getter
public class SStmt {
    private final Stmt unit;

    public SStmt(Stmt unit) {
        this.unit = unit;
    }

    @Override
    public boolean equals(Object other) {
        return this.unit.equals(other);
    }

    @Override
    public String toString() {
        return unit.toString();
    }
}
