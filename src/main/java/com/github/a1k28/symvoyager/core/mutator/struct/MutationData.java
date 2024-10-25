package com.github.a1k28.symvoyager.core.mutator.struct;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MutationData<T> {
    private final MType type;
    private final Class<T> mutant;
}
