package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.graph.BasicBlock;

@Getter
@RequiredArgsConstructor
public class StmtCache {
    private final SType type;
    private final BasicBlock<?> block;
}
