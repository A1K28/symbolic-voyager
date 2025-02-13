package com.github.a1k28.symvoyager.core.z3extended.model;

import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IteratorInstanceModel {
    private Expr reference;
    private Type type;
    private IntExpr index;
    private IntExpr size;
    private Expr base;

    public enum Type {
        LIST,
        UNKNOWN
    }
}
