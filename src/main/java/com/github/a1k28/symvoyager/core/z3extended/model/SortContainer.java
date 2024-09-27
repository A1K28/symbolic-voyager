package com.github.a1k28.symvoyager.core.z3extended.model;

import com.github.a1k28.symvoyager.core.z3extended.sort.AbstractSort;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SortContainer<T extends AbstractSort> {
    private final T sort;
    private final Expr sentinel;
}
