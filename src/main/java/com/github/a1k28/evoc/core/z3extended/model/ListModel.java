package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ListModel {
    private final Expr expr;
    private final Sort sort;
    private final List<Expr> arguments;
}
