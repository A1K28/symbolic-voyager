package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.Expr;
import com.microsoft.z3.TupleSort;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SortContainer {
    private final TupleSort sort;
    private final Expr sentinel;
}
