package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class MethodMockResult {
    private final Method method;
    private final Object parsedReturnValue;
    private final Class returnType;
    private final Class exceptionType;
    private final List<Object> parsedParameters;
}
