package com.github.a1k28.evoc.core.z3extended.model;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import lombok.*;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class ListModel {
    private ArrayExpr expr;
    private final Sort sort;
    private final List<Expr> arguments;
    private final Expr sentinel;
}
