package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Position;

@Getter
@RequiredArgsConstructor
public class StmtWithPosition {
    private final Stmt handlerStmt;
    private final Position position;
}
