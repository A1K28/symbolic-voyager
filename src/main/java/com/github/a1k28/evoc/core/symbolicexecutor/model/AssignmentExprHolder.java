package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.microsoft.z3.Expr;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignmentExprHolder {
    private final Expr left;
    private final Expr right;
}
