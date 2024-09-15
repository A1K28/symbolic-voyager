package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;
import java.util.List;

@Setter
@Getter
@ToString
public class SMethodMockVar extends SVar {
    private final Method method;
    private final List<Expr> arguments;

    public SMethodMockVar(String name, Expr expr, VarType type, Class<?> classType,
                          boolean isDeclaration, Method method, List<Expr> arguments) {
        super(name, expr, type, classType, isDeclaration);
        this.method = method;
        this.arguments = arguments;
    }
}
