package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.microsoft.z3.Expr;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class SVar {
    private final String name;
    private Expr expr;
    private final VarType type;
    private Class<?> classType;
    private final boolean isDeclaration;

    public SVar(String name, Expr expr, VarType type, Class<?> classType, boolean isDeclaration) {
        if (type == null) throw new RuntimeException("null VarType provided");
        this.name = name;
        this.expr = expr;
        this.type = type;
        this.classType = classType;
        this.isDeclaration = isDeclaration;
    }
}
