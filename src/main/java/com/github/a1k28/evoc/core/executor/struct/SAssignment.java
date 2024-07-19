package com.github.a1k28.evoc.core.executor.struct;

import com.github.a1k28.evoc.core.executor.model.SType;
import lombok.Getter;

@Getter
public class SAssignment extends SExpr {
    private final SExpr left;
    private final SExpr right;

    public SAssignment(SExpr left, SExpr right) {
        super(SType.ASSIGNMENT);
        this.left = left;
        this.right = right;
    }
}
