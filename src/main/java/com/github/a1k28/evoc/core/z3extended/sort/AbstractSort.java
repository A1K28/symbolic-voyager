package com.github.a1k28.evoc.core.z3extended.sort;

import com.microsoft.z3.TupleSort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractSort {
    protected final TupleSort sort;

    public TupleSort getSort() {
        return this.sort;
    }
}
