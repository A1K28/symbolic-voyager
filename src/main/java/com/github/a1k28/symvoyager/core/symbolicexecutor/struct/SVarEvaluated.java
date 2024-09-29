package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class SVarEvaluated {
    private final SVar svar;
    private final Object evaluated;
}
