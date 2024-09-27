package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import lombok.Getter;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ClassInstanceVar<T> {
    private final Class<T> clazz;
    private final Map<String, Object> fields;
    private final Constructor<T> constructor;
    private final Object[] constructorArgs;

    // TODO: stop assuming default constructor
    public ClassInstanceVar(Class<T> clazz) {
        this.clazz = clazz;
        this.fields = new HashMap<>();

        try {
            this.constructor = clazz.getDeclaredConstructor();
            this.constructorArgs = new Object[constructor.getParameterCount()];
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
