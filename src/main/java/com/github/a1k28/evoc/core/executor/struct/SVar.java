package com.github.a1k28.evoc.core.executor.struct;

import com.microsoft.z3.Expr;
import lombok.Getter;
import sootup.core.jimple.basic.Value;

@Getter
public class SVar {
    private final String name;
    private final Value value;
    private final Expr expr;
    private final VarType type;
    private final boolean isOriginal;

    public SVar(String name, Value value, Expr expr, VarType type, boolean isOriginal) {
        if (type == null) throw new RuntimeException("Null VarType provided");
        this.name = name;
        this.value = value;
        this.expr = expr;
        this.type = type;
        this.isOriginal = isOriginal;
    }

    public SVar(SVar sVar, VarType varType) {
        this(sVar.getName(), sVar.getValue(), sVar.getExpr(), varType, sVar.isOriginal);
    }

    public static SVar renew(SVar sVar) {
        return new SVar(
                sVar.getName(),
                sVar.getValue(),
                sVar.getExpr(),
                sVar.getType(),
                true
        );
    }
}
