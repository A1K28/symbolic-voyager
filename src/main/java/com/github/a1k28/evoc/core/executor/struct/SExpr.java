package com.github.a1k28.evoc.core.executor.struct;

import com.github.a1k28.evoc.core.executor.model.SType;
import com.microsoft.z3.Expr;
import lombok.Getter;

@Getter
public class SExpr {
    private final Expr expr;
    private final SType sType;

    public SExpr(Expr expr) {
        this.expr = expr;
        this.sType = SType.OTHER;
    }

    public SExpr(SType sType) {
        this.expr = null;
        this.sType = sType;
    }

    public SExpr(Expr expr, SType sType) {
        this.expr = expr;
        this.sType = sType;
    }

    public SMethodExpr asMethod() {
        return (SMethodExpr) this;
    }
}
