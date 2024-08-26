package com.github.a1k28.evoc.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EvaluatedResult {
    private final Object returnValue;
    private final Object[] evaluatedParameters;
}
