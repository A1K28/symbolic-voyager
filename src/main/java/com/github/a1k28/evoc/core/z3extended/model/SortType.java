package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SortType {
    ARRAY("Array"),
    MAP("Map"),
    OBJECT("Object");

    private final String value;

    public Sort value(Context ctx) {
        return ctx.mkUninterpretedSort(this.value);
    }

    public boolean equals(Sort sort) {
        return this.value.equals(sort.toString());
    }
}