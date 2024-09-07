package com.github.a1k28.evoc.core.cli.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class CLIOptions {
    private Set<PropagationStrategy> propagationStrategies;
}
