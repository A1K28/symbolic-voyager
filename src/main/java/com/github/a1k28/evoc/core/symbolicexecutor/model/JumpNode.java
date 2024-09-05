package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JumpNode {
    private final SMethodPath methodPath;
    private final SNode node;
    private boolean isThrows;
}
