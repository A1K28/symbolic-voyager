package com.github.a1k28.evoc.core.executor.struct;

import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

import java.util.List;

@Getter
public class SMethodExpr extends SExpr {
    private final AbstractInvokeExpr invokeExpr;
    private final List<Value> args;
    private final boolean isUnknown;

    public SMethodExpr(AbstractInvokeExpr invokeExpr, List<Value> args, boolean isUnknown) {
        super(SType.INVOKE);
        this.invokeExpr = invokeExpr;
        this.args = args;
        this.isUnknown = isUnknown;
    }
}
