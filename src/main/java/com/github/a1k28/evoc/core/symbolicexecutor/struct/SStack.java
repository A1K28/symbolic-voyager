package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.helper.SootHelper;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.Expr;
import sootup.core.jimple.basic.Value;

import java.util.*;
import java.util.stream.Collectors;

public class SStack implements IStack {
    private int index;
    private final List<Map<String, List<SVar>>> stack;

    public SStack() {
        this.index = 0;
        this.stack = new ArrayList<>();
        this.stack.add(new LinkedHashMap<>());
    }

    public void clear() {
        this.index = 0;
        this.stack.clear();
        this.stack.add(new LinkedHashMap<>());
    }

    public Optional<SVar> get(String key) {
        for (int i = index; i >= 0; i--) {
            if (stack.get(i).containsKey(key)) {
                List<SVar> vars = stack.get(i).get(key);
                return Optional.ofNullable(vars.get(vars.size()-1));
            }
        }
        return Optional.empty();
    }

    public SVar add(String name, Value value, Expr expr, VarType type) {
        Optional<SVar> optional = getDeclaration(name);
        SVar sVar;
        Class<?> classType = SootHelper.translateType(value.getType());
        if (optional.isPresent()) {
            sVar = new SVar(name, value, expr, type, classType, false);
        }
        else {
            sVar = new SVar(name, value, expr, type, classType, true);
        }
        _add(sVar);
        return sVar;
    }

    public List<SVar> getAll() {
        return stack.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void push() {
        stack.add(new LinkedHashMap<>());
        index++;
    }

    @Override
    public void pop() {
        stack.remove(index);
        index--;
    }

    private void _add(SVar sVar) {
        if (!stack.get(index).containsKey(sVar.getName()))
            stack.get(index).put(sVar.getName(), new ArrayList<>());
        stack.get(index).get(sVar.getName()).add(sVar);
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
