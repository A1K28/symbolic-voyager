package com.github.a1k28.evoc.core.symbolicexecutor.model;

import com.microsoft.z3.Status;

public enum Z3Status {
    SATISFIABLE,
    SATISFIABLE_END,
    UNSATISFIABLE_END;

    public static Z3Status from(Status status) {
        return switch (status) {
            case SATISFIABLE -> SATISFIABLE;
            case UNKNOWN, UNSATISFIABLE -> UNSATISFIABLE_END;
        };
    }
}
