package com.github.a1k28.evoc.core.executor.struct;

import com.github.a1k28.evoc.core.executor.model.SType;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

import java.util.List;

@Getter
public class SMethodExpr extends SExpr {
    private final AbstractInvokeExpr invokeExpr;
    private final Value base;
    private final List<Value> args;
    private final boolean invokable;

    public SMethodExpr(AbstractInvokeExpr invokeExpr, Value base, List<Value> args, boolean invokable) {
        super(SType.INVOKE);
        this.invokeExpr = invokeExpr;
        this.args = args;
        this.base = base;
        this.invokable = invokable;
    }
}
