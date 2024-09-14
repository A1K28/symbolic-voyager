package com.github.a1k28.evoc.core.symbolicexecutor.model;

public enum SType {
    ROOT,
    BRANCH,
    BRANCH_TRUE,
    BRANCH_FALSE,
    ASSIGNMENT,
    PARAMETER,
    IDENTITY,
    INVOKE,
    INVOKE_SPECIAL_CONSTRUCTOR,
    INVOKE_MOCK,
    SWITCH,
    THROW,
    ENTER_MONITOR,
    EXIT_MONITOR,
    GOTO,
    RETURN,
    RETURN_VOID,
    NOP,
    BREAKPOINT,
    OTHER
}
