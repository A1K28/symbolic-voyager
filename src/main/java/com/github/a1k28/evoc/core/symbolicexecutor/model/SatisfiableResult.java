package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVar;
import com.microsoft.z3.BoolExpr;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
public class SatisfiableResult {
    private final List<BoolExpr> z3Assertions;
    private final List<SVar> fields;
    private final SVar returnValue;
    private final boolean continuable;
    private final Map<SVar, String> symbolicParameterValues;

    public SatisfiableResult(BoolExpr[] z3Assertions,
                             List<SVar> fields,
                             SVar returnValue,
                             boolean continuable,
                             Map<SVar, String> symbolicParameterValues) {
        this.z3Assertions = Arrays.asList(z3Assertions);
        this.fields = fields;
        this.returnValue = returnValue;
        this.continuable = continuable;
        this.symbolicParameterValues = symbolicParameterValues;
    }
}
