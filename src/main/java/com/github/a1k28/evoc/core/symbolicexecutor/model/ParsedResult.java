package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ParsedResult {
    private final Object parsedReturnValue;
    private final Object[] parsedParameters;
    private final List<SVarEvaluated> parsedFields;
    private final List<MethodMockResult> methodMockValues;
    private final Class exceptionType;
}
