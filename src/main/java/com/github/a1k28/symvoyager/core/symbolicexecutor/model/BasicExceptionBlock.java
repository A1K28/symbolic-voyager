package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.graph.BasicBlock;
import sootup.core.types.ClassType;

@Getter
@RequiredArgsConstructor
public class BasicExceptionBlock {
    private final ClassType exceptionType;
    private final BasicBlock<?> block;
    private final SMethodPath methodPath;
}
