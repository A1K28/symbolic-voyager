package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import com.microsoft.z3.BoolExpr;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class SatisfiableResult {
    private final List<BoolExpr> z3Assertions;
    private final List<SVarEvaluated> symbolicFieldValues;
    private final List<SVarEvaluated> symbolicParameterValues;
    private final SVarEvaluated returnValue;
    private final boolean continuable;

    public SatisfiableResult(BoolExpr[] z3Assertions,
                             List<SVarEvaluated> symbolicFieldValues,
                             List<SVarEvaluated> symbolicParameterValues,
                             SVarEvaluated returnValue,
                             boolean continuable) {
        this.z3Assertions = Arrays.asList(z3Assertions);
        this.symbolicFieldValues = symbolicFieldValues;
        this.symbolicParameterValues = symbolicParameterValues;
        this.returnValue = returnValue;
        this.continuable = continuable;
    }

    public Object getParameter(String name) {
        return symbolicParameterValues.stream()
                .filter(e -> name.equals(e.getSvar().getName()))
                .map(SVarEvaluated::getEvaluated)
                .findFirst().orElse(null);
    }
}
