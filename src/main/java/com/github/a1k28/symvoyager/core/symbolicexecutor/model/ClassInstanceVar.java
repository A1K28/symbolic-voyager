package com.github.a1k28.symvoyager.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ClassInstanceVar<T> {
    private final Class<T> clazz;
    private final Map<String, Object> fields;
    private final Constructor<T> constructor;
    private final Object[] constructorArgs;
    private final boolean isStub;

    public ClassInstanceVar(Class<T> clazz, boolean isStub) {
        this.isStub = isStub;
        if (isStub) {
            this.clazz = clazz;
            this.fields = null;
            this.constructor = null;
            this.constructorArgs = null;
        } else {
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

    // TODO: stop assuming default constructor
    public ClassInstanceVar(Class<T> clazz) {
        this(clazz, false);
    }
}
