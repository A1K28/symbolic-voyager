package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.symbolicexecutor.SymbolicExecutor;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.symvoyager.core.z3extended.Z3Translator;
import lombok.Getter;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.HashMap;
import java.util.Map;

public class SymbolicHandlerContext {
    @Getter
    private final SymbolicExecutor se;
    @Getter
    private final Z3Translator z3t;
    @Getter
    private final Z3ExtendedContext ctx;
    private final Map<SType, AbstractSymbolicHandler> handlerMap = new HashMap<>();

    public SymbolicHandlerContext(
            SymbolicExecutor se,
            Z3Translator z3t,
            Z3ExtendedContext ctx) {
        this.se = se;
        this.z3t = z3t;
        this.ctx = ctx;

        BranchHandler branchHandler = new BranchHandler(this);
        ReturnHandler returnHandler = new ReturnHandler(this);

        this.handlerMap.put(SType.ASSIGNMENT, new AssignmentHandler(this));
        this.handlerMap.put(SType.BRANCH, branchHandler);
        this.handlerMap.put(SType.GOTO, new GotoHandler(this));
        this.handlerMap.put(SType.PARAMETER, new ParameterHandler(this));
        this.handlerMap.put(SType.RETURN, returnHandler);
        this.handlerMap.put(SType.RETURN_VOID, returnHandler);
        this.handlerMap.put(SType.THROW, new ThrowHandler(this));
        this.handlerMap.put(SType.INVOKE, new VoidMethodCallHandler(this));
    }

    public SType handle(SMethodPath methodPath, Stmt stmt) throws ClassNotFoundException {
        SType type = methodPath.getType(stmt);
        if (handlerMap.containsKey(type))
            return handlerMap.get(type).handle(methodPath, stmt);
        return SType.OTHER;
    }
}
