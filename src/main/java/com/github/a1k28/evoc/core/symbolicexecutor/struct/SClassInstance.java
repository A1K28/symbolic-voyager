package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class SClassInstance {
    private final Class<?> clazz;
    private final Set<String> fields;
    private final Map<Method, SMethodPath> methodPathSkeletons;

    public SClassInstance(Class<?> clazz) {
        this.fields = new HashSet<>();
        this.methodPathSkeletons = new HashMap<>();
        this.clazz = clazz;
    }

    public String getClassname() {
        return this.clazz.getName();
    }
}
