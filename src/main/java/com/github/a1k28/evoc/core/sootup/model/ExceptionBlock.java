package com.github.a1k28.evoc.core.sootup.model;

import lombok.*;
import sootup.core.graph.BasicBlock;
import sootup.core.types.ClassType;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionBlock {
    private ClassType type;
    private BasicBlock block;
}
