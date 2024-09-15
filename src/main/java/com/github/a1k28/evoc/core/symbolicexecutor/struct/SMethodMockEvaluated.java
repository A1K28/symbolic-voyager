package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;

@Getter
public class SMethodMockEvaluated extends SVarEvaluated {
    private final Method method;
    private final Class exceptionType;
    private final List<Object> parametersEvaluated; // in case of method mocks

    public SMethodMockEvaluated(SVar sVar,
                                Object returnValue,
                                List<Object> parametersEvaluated,
                                Class exceptionType,
                                Method method) {
        super(sVar, returnValue);
        this.method = method;
        this.exceptionType = exceptionType;
        this.parametersEvaluated = parametersEvaluated;
    }
}
