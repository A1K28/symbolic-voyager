package com.github.a1k28.symvoyager.core.cli.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class GetterSetterAnalyzer {
    public static boolean isGetterOrSetter(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && !"void".equals(method.getReturnType().toString())) {
            return extractField(method).isPresent();
        }
        if (name.startsWith("set") && "void".equals(method.getReturnType().toString())) {
            return extractField(method).isPresent();
        }
        return false;
    }

    private static Optional<Field> extractField(Method method) {
        String fieldName = deCapitalize(method.getName().substring(3));
        return extractField(method.getDeclaringClass(), fieldName);
    }

    private static Optional<Field> extractField(Class clazz, String fieldName) {
        if (clazz == null) return Optional.empty();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName))
                return Optional.of(field);
        }
        return extractField(clazz.getSuperclass(), fieldName);
    }

    private static String deCapitalize(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }
}