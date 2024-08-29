package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class KeyExprs {
    private List<Expr> keys;
    private List<BoolExpr> wasInitiallyPresent;

    public KeyExprs() {
        this.keys = new ArrayList<>();
        this.wasInitiallyPresent = new ArrayList<>();
    }

    public void add(Expr key, BoolExpr wasInitiallyPresent) {
        if (!this.keys.contains(key)) {
            this.keys.add(key);
            this.wasInitiallyPresent.add(wasInitiallyPresent);
        }
    }
}
