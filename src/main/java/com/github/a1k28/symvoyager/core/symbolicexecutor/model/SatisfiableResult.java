package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodMockEvaluated;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVarEvaluated;
import com.microsoft.z3.BoolExpr;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class SatisfiableResult {
    private final List<BoolExpr> z3Assertions;
    private final List<SVarEvaluated> symbolicFieldValues;
    private final List<SVarEvaluated> symbolicParameterValues;
    private final List<SMethodMockEvaluated> mockedMethodValues;
    private final SVarEvaluated returnValue;
    private final Class<? extends Throwable> exceptionType;
    private final List<SNode> path;

    public SatisfiableResult(BoolExpr[] z3Assertions,
                             List<SVarEvaluated> symbolicFieldValues,
                             List<SVarEvaluated> symbolicParameterValues,
                             List<SMethodMockEvaluated> mockedMethodValues,
                             SVarEvaluated returnValue,
                             Class<? extends Throwable> exceptionType,
                             List<SNode> path) {
        this.z3Assertions = Arrays.asList(z3Assertions);
        this.symbolicFieldValues = symbolicFieldValues;
        this.symbolicParameterValues = symbolicParameterValues;
        this.mockedMethodValues = mockedMethodValues;
        this.returnValue = returnValue;
        this.exceptionType = exceptionType;
        this.path = path;
    }

    public Object getParameter(String name) {
        List<SVarEvaluated> results = symbolicParameterValues.stream()
                .filter(e -> name.equals(e.getSvar().getName()))
                .limit(1)
                .toList();
        if (results.isEmpty())
            return null;
        return results.get(0).getEvaluated();
    }
}
