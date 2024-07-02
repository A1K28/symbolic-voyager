package com.github.a1k28.evoc.core.symbex.struct;

import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SPath {
    private final SNode root;
    private final Map<String, JParameterRef> nameToParamIdx;

    public SPath() {
        this.root = new SNode();
        this.nameToParamIdx = new HashMap<>();
    }

    public SNode createNode(Stmt unit) {
        return new SNode(unit, getType(unit));
    }

    public void print() {
        this.root.print(1);
        System.out.println();
    }

    private SType getType(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JAssignStmt.class) return SType.ASSIGNMENT;
        if (clazz == JGotoStmt.class) return SType.GOTO;
        if (clazz == JRetStmt.class) return SType.RETURN;
        if (clazz == JReturnStmt.class) return SType.RETURN;
        if (clazz == JInvokeStmt.class) return SType.INVOKE;
        if (clazz == JSwitchStmt.class) return SType.SWITCH;
        if (clazz == JThrowStmt.class) return SType.THROW;
        if (clazz == JEnterMonitorStmt.class) return SType.ENTER_MONITOR;
        if (clazz == JExitMonitorStmt.class) return SType.EXIT_MONITOR;
        if (clazz == JReturnVoidStmt.class) return SType.RETURN_VOID;
        if (clazz == JNopStmt.class) return SType.NOP;
        if (clazz == JBreakpointStmt.class) return SType.BREAKPOINT;
        if (unit instanceof JIdentityStmt u) {
            Value val = u.getRightOp();
            if (val instanceof JParameterRef v) {
                this.nameToParamIdx.put(u.getLeftOp().toString(), v);
                return SType.PARAMETER;
            }
            return SType.IDENTITY;
        }
        System.out.println("Could not identify: " + unit);
        return SType.OTHER;
    }
}
