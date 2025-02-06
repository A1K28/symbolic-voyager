package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sootup.core.graph.BasicBlock;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandlerNode {
    private SMethodPath methodPath;
    private BasicBlock<?> block;
}
