package com.github.a1k28.symvoyager.core.z3extended.struct;

import com.github.a1k28.symvoyager.core.z3extended.model.IStack;

import java.util.*;
import java.util.stream.Collectors;

public class Z3Stack<K,V> implements IStack {
    protected int index;
    protected final List<Map<K,List<V>>> stack;

    public Z3Stack() {
        this.index = 0;
        this.stack = new ArrayList<>();
        this.stack.add(Collections.emptyMap());
    }

    @Override
    public void push() {
        stack.add(Collections.emptyMap());
        index++;
    }

    @Override
    public void pop() {
        stack.remove(index);
        index--;
    }

    public void add(K key, V value) {
        // optimize
        if (stack.get(index).isEmpty()) {
            stack.remove(index);
            stack.add(new HashMap<>());
        }

        // add
        if (!this.stack.get(index).containsKey(key))
            this.stack.get(index).put(key, new ArrayList<>());
        this.stack.get(index).get(key).add(value);
    }

    public Optional<V> get(K key) {
        for (int i = stack.size()-1; i >= 0; i--) {
            if (stack.get(i).containsKey(key))
                return Optional.of(stack.get(i).get(key).get(stack.get(i).get(key).size()-1));
        }
        return Optional.empty();
    }

    public Optional<V> getFirst(K key) {
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i).containsKey(key))
                return Optional.of(stack.get(i).get(key).get(0));
        }
        return Optional.empty();
    }

    public List<V> getAll() {
        return stack.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
