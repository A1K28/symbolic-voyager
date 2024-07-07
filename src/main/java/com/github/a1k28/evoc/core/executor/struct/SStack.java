package com.github.a1k28.evoc.core.executor.struct;

import java.util.*;
import java.util.stream.Collectors;

public class SStack {
    private int index;
    private final List<Map<String, SVar>> stack;

    public SStack() {
        this.index = 0;
        this.stack = new ArrayList<>();
        this.stack.add(new HashMap<>());
    }

    public Optional<SVar> get(String key) {
        for (int i = index; i >= 0; i--) {
            if (stack.get(i).containsKey(key))
                return Optional.ofNullable(stack.get(i).get(key));
        }
        return Optional.empty();
    }

    public void add(SVar sVar) {
        stack.get(index).put(sVar.getName(), sVar);
    }

    public void update(String oldKey, String newKey) {
        for (int i = index; i >= 0; i--) {
            if (stack.get(i).containsKey(oldKey)) {
                stack.get(i).put(newKey, stack.get(i).get(oldKey));
                stack.get(i).remove(oldKey);
            }
        }
    }

    public List<SVar> getAll() {
        return stack.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
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
}
