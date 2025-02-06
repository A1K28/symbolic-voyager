package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.graph.BasicBlock;

@Getter
@RequiredArgsConstructor
public class JumpNode {
    private final SMethodPath methodPath;
    private final BasicBlock<?> block;
    private final int index;
}
