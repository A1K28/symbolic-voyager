package com.github.a1k28.evoc.core.executor.struct;

import com.microsoft.z3.Expr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sootup.core.jimple.basic.Value;

@Getter
@Setter
@AllArgsConstructor
public class SVar {
    private String name;
    private final Value value;
    private final Expr exrp;
    private final boolean isOriginal;
}
