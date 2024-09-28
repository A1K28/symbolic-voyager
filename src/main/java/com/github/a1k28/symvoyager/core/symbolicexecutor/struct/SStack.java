package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.VarType;
import com.github.a1k28.symvoyager.core.z3extended.struct.Z3Stack;
import com.github.a1k28.symvoyager.core.z3extended.model.IStack;
import com.microsoft.z3.Expr;

import java.util.*;
import java.util.stream.Collectors;

public class SStack extends Z3Stack<String, SVar> implements IStack {
    public Optional<SVar> get(String key) {
        for (int i = index; i >= 0; i--) {
            if (stack.get(i).containsKey(key)) {
                List<SVar> vars = stack.get(i).get(key);
                return Optional.ofNullable(vars.get(vars.size()-1));
            }
        }
        return Optional.empty();
    }

    public Optional<SVar> get(Expr expr) {
        List<SVar> res = getAll().stream().filter(var -> expr.equals(var.getExpr())).collect(Collectors.toList());
        if (res.isEmpty()) return Optional.empty();
        return Optional.of(res.get(res.size()-1));
    }

    public SVar add(String name, Class<?> classType, Expr expr, VarType type) {
        Optional<SVar> optional = getDeclaration(name);
        SVar sVar;
        if (optional.isPresent()) {
            sVar = new SVar(name, expr, type, classType, false);
        }
        else {
            sVar = new SVar(name, expr, type, classType, true);
        }
        add(sVar);
        return sVar;
    }

    public void add(SVar sVar) {
        this.add(sVar.getName(), sVar);
    }

    @Override
    public void add(String key, SVar sVar) {
        // optimize
        if (stack.get(index).isEmpty()) {
            stack.remove(index);
            stack.add(new LinkedHashMap<>());
        }

        // add
        if (!stack.get(index).containsKey(key))
            stack.get(index).put(key, new ArrayList<>());
        stack.get(index).get(key).add(sVar);
    }

    public Optional<SVar> getDeclaration(String key) {
        for (int i = 0; i <= index; i++) {
            if (stack.get(i).containsKey(key)) {
                for (SVar var : stack.get(i).get(key))
                    if (var.isDeclaration())
                        return Optional.ofNullable(var);
            }
        }
        return Optional.empty();
    }
}
