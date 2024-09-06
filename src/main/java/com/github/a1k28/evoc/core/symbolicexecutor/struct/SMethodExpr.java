package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

import java.lang.reflect.Method;
import java.util.List;

@Getter
public class SMethodExpr extends SExpr {
    private final AbstractInvokeExpr invokeExpr;
    private final Value base;
    private final List<Value> args;
    private final Method method;
    private final boolean shouldPropagate; // false means modelled, true means propagation

    public SMethodExpr(AbstractInvokeExpr invokeExpr,
                       Value base,
                       List<Value> args,
                       Method method,
                       boolean shouldPropagate) {
        super(SType.INVOKE);
        this.invokeExpr = invokeExpr;
        this.args = args;
        this.base = base;
        this.method = method;
        this.shouldPropagate = shouldPropagate;
    }

    public boolean shouldPropagate() {
        return this.shouldPropagate;
    }
}
