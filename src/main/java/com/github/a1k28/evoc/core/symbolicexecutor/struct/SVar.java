package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import sootup.core.jimple.basic.Value;

import java.lang.reflect.Method;

@Setter
@Getter
@ToString
public class SVar {
    private final String name;
    private final Value value;
    private final Expr expr;
    private final VarType type;
    private final Class<?> classType;
    private final boolean isDeclaration;

    public SVar(String name, Value value, Expr expr, VarType type, Class<?> classType, boolean isDeclaration) {
        if (type == null) throw new RuntimeException("Null VarType provided");
        this.name = name;
        this.value = value;
        this.expr = expr;
        this.type = type;
        this.classType = classType;
        this.isDeclaration = isDeclaration;
    }
}
