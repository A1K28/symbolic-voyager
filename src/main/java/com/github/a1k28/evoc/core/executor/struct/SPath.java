package com.github.a1k28.evoc.core.executor.struct;

import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import lombok.Getter;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.model.SootClass;
import sootup.java.core.JavaSootField;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SPath {
    private static final Logger log = Logger.getInstance(SPath.class);

    private final SNode root;
    private final Map<String, SParam> nameToParamIdx;

    public SPath() {
        this.root = new SNode();
        this.nameToParamIdx = new HashMap<>();
    }

    public SNode createNode(Stmt unit) {
        return new SNode(unit, getType(unit));
    }

    public void print() {
        this.root.print(1);
        log.empty();
    }

    public SParam getParam(String key) {
        if (key.contains(": "))
            key = key.substring(key.indexOf(": "), key.length()-1);
        if (nameToParamIdx.containsKey(key))
            return nameToParamIdx.get(key);
        return null;
    }

    public void addFields(SootClass sootClass) throws ClassNotFoundException {
        sootClass.getFields().forEach(e -> {if (e instanceof JavaSootField j) addField(j);});
        if (sootClass.hasSuperclass()) {
            String name = sootClass.getSuperclass().get().toString();
            if (!name.equals(Object.class.getName())) {
                SootClass parent = SootHelper.getSootClass(sootClass.getSuperclass().get().toString());
                addFields(parent);
            }
        }
    }

    private void addField(JavaSootField field) {
        String key = field.toString();
        if (key.contains(": "))
            key = key.substring(key.indexOf(": "), key.length()-1);
        this.nameToParamIdx.put(key, new SParam());
    }

    private SType getType(Stmt unit) {
        Class<? extends Stmt> clazz = unit.getClass();
        if (clazz == JIfStmt.class) return SType.BRANCH;
        if (clazz == JGotoStmt.class) return SType.GOTO;
        if (clazz == JRetStmt.class) return SType.RETURN;
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
            if (val instanceof JParameterRef v) {
                this.nameToParamIdx.put(u.getLeftOp().toString(), new SParam(v));
                return SType.PARAMETER;
            }
            return SType.IDENTITY;
        }
        log.warn("Could not identify: " + unit);
        return SType.OTHER;
    }
}
