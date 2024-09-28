package com.github.a1k28.symvoyager.core.z3extended.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SClassInstance;
import com.microsoft.z3.Expr;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class ClassInstanceModel {
    private final Expr expr;
    private final Expr base;
    private final SClassInstance classInstance;
    private final boolean isStub;
}
