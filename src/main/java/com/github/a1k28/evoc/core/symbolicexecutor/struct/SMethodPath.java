package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.helper.Logger;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;

import java.util.HashSet;
import java.util.Set;

@Getter
public class SMethodPath {
    private static final Logger log = Logger.getInstance(SMethodPath.class);

    private final SNode root;
    private final Set<String> fields;
    private final Class<?> clazz;
    private final SParamList paramList;

    public SMethodPath(Class<?> clazz, SParamList paramList) {
        this.root = new SNode();
        this.fields = new HashSet<>();
        this.clazz = clazz;
        this.paramList = paramList;
    }

    public SNode createNode(Stmt unit) {
        SStmt u = new SStmt(unit);
        return new SNode(u, getType(unit));
    }

    public void print() {
        this.root.print(2);
        log.empty();
    }

    private SType getType(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JRetStmt.class) return SType.RETURN;
        if (clazz == JGotoStmt.class) return SType.GOTO;
        if (clazz == JAssignStmt.class) return SType.ASSIGNMENT;
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
            if (val instanceof JParameterRef) {
                return SType.PARAMETER;
            }
            return SType.IDENTITY;
        }
        log.warn("Could not identify: " + unit);
        return SType.OTHER;
    }

    public String getClassname() {
        return this.clazz.getName();
    }
}
