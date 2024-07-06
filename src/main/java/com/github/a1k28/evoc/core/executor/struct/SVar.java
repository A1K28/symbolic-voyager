package com.github.a1k28.evoc.core.executor.struct;

import com.microsoft.z3.Expr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sootup.core.jimple.basic.Value;

@Getter
@AllArgsConstructor
public class SVar {
    private final String name;
    private final Value value;
    private final Expr exrp;
    private final boolean isOriginal;
}
