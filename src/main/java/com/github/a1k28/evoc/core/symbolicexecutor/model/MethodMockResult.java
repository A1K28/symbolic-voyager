package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class MethodMockResult {
    private final Method method;
    private final Object parsedReturnValue;
    private final Class exceptionType;
    private final List<Object> parsedParameters;
}