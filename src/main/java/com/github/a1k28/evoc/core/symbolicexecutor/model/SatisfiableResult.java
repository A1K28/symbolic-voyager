package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVar;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import com.microsoft.z3.BoolExpr;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
public class SatisfiableResult {
    private final List<BoolExpr> z3Assertions;
    private final List<SVar> fields;
    private final SVarEvaluated returnValue;
    private final boolean continuable;
    private final List<SVarEvaluated> symbolicParameterValues;

    public SatisfiableResult(BoolExpr[] z3Assertions,
                             List<SVar> fields,
                             SVarEvaluated returnValue,
                             boolean continuable,
                             List<SVarEvaluated> symbolicParameterValues) {
        this.z3Assertions = Arrays.asList(z3Assertions);
        this.fields = fields;
        this.returnValue = returnValue;
        this.continuable = continuable;
        this.symbolicParameterValues = symbolicParameterValues;
    }

    public Object getParameter(String name) {
        return symbolicParameterValues.stream()
                .filter(e -> name.equals(e.getSvar().getName()))
                .map(SVarEvaluated::getEvaluated)
                .findFirst().orElse(null);
    }
}
