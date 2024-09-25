package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import lombok.Getter;
import lombok.Setter;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SCatchNode extends SNode {
    private ClassType exceptionType;

    public SCatchNode(Stmt unit, ClassType exceptionType) {
        super(unit, SType.CATCH);
        this.exceptionType = exceptionType;
    }

    @Override
    public String toString() {
        return this.type + " " + unit + " " + exceptionType;
    }
}
