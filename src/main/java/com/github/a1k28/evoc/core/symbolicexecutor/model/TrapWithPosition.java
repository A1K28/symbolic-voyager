package com.github.a1k28.evoc.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.jimple.basic.Trap;
import sootup.core.model.Position;

@Getter
@RequiredArgsConstructor
public class TrapWithPosition {
    private final Trap trap;
    private final Position position;
}
