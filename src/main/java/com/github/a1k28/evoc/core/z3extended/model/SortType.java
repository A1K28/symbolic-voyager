package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SortType {
    NULL("Null"),
    ARRAY("Array"),
    MAP("Map"),
    REFERENCE("reference"),
    SET("SET"),
    OBJECT("Object");

    private final String value;

    public Sort value(Context ctx) {
        return ctx.mkUninterpretedSort(this.value);
    }

    public boolean equals(Sort sort) {
        return this.value.equals(sort.toString());
    }
}
