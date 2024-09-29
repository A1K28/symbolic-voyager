package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVarEvaluated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ParsedResult {
    private final Object parsedReturnValue;
    private final Class returnType;
    private final Object[] parsedParameters;
    private final List<SVarEvaluated> parsedFields;
    private final List<MethodMockResult> methodMockValues;
    private final Class<? extends Throwable> exceptionType;
}
