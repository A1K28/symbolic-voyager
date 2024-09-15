package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.microsoft.z3.Expr;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

import java.util.List;

@Getter
public class SMethodExpr extends SExpr {
    private final AbstractInvokeExpr invokeExpr;
    private final Value base;
    private final List<Value> args;
    private final boolean shouldPropagate; // false means modelled, true means propagation

    public SMethodExpr(Expr value,
                       AbstractInvokeExpr invokeExpr,
                       Value base,
                       List<Value> args,
                       boolean shouldPropagate) {
        this(value, SType.INVOKE, invokeExpr, base, args, shouldPropagate);
    }

    public SMethodExpr(Expr value,
                       SType sType,
                       AbstractInvokeExpr invokeExpr,
                       Value base,
                       List<Value> args,
                       boolean shouldPropagate) {
        super(value, sType);
        this.invokeExpr = invokeExpr;
        this.args = args;
        this.base = base;
        this.shouldPropagate = shouldPropagate;
    }

    public boolean shouldPropagate() {
        return this.shouldPropagate;
    }
}
