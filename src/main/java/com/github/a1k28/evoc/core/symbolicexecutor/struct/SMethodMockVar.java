package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import sootup.core.types.ClassType;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@Setter
@ToString
public class SMethodMockVar extends SVar {
    private final Method method;
    private final List<Expr> arguments;
    private ClassType throwType;

    public SMethodMockVar(String name, Expr expr, VarType type, Class<?> classType,
                          boolean isDeclaration, Method method, List<Expr> arguments) {
        super(name, expr, type, classType, isDeclaration);
        this.method = method;
        this.arguments = arguments;
    }

    public SMethodMockVar copy() {
        return new SMethodMockVar(this.getName(), this.getExpr(), this.getType(),
                this.getClassType(), this.isDeclaration(), this.getMethod(), this.getArguments());
    }
}
