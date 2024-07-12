package com.github.a1k28.evoc.core.executor.struct;

import com.microsoft.z3.Expr;
import sootup.core.jimple.basic.Value;

import java.util.*;
import java.util.stream.Collectors;

public class SStack {
    private int index;
    private final List<Map<String, List<SVar>>> stack;

    public SStack() {
        this.index = 0;
        this.stack = new ArrayList<>();
        this.stack.add(new HashMap<>());
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

    public void add(SVar sVar) {
        add(sVar.getName(), sVar.getValue(), sVar.getExpr(), sVar.getType());
    }

    public SVar add(String name, Value value, Expr expr, VarType type) {
//        Optional<SVar> existing = find(value, expr);
//        if (existing.isPresent()) return existing.get();

        Optional<SVar> optional = getDeclaration(name);
        SVar sVar;
        if (optional.isPresent()) {
            sVar = new SVar(name, value, expr, type, false);
        }
        else {
            sVar = new SVar(name, value, expr, type, true);
        }
        _add(sVar);
        return sVar;
    }

    public void update(String oldKey, String newKey) {
        for (int i = index; i >= 0; i--) {
            if (stack.get(i).containsKey(oldKey)) {
                stack.get(i).put(newKey, stack.get(i).get(oldKey));
                stack.get(i).remove(oldKey);
                return;
            }
        }
    }

    public List<SVar> getAll() {
        return stack.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
//                .map()
                .collect(Collectors.toList());
    }

    public List<SVar> getFields() {
        return stack.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .filter(e -> e.getType() == VarType.FIELD)
                .collect(Collectors.toList());
    }

    public void push() {
        stack.add(new HashMap<>());
        index++;
    }

    public void pop() {
        stack.remove(index);
        index--;
    }

    private Optional<SVar> find(Value value, Expr expr) {
        return getAll().stream().filter(e -> e.getValue().toString().equals(value.toString())
                && e.getExpr().equals(expr)).findFirst();
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
