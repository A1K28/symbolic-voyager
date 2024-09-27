package com.github.a1k28.symvoyager.core.z3extended.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SortType {
    NULL("null"),
    ARRAY("array"),
    MAP("map"),
    REFERENCE("reference"),
    SET("set"),
    OBJECT("object"),
    SENTINEL("sentinel"),
    UNKNOWN("unknown");

    private final String value;

    public Sort value(Context ctx) {
        return ctx.mkUninterpretedSort(this.value);
    }

    public boolean equals(Sort sort) {
        return this.value.equals(sort.toString());
    }
}
