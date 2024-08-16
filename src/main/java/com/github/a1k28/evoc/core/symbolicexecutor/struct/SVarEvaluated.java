package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SVarEvaluated {
    private final SVar svar;
    private final Object evaluated;
}
