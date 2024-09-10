package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class SymbolTranslator {
    public static Map<SatisfiableResult, ParsedResult> parse(SatisfiableResults satisfiableResults) {
        Class<?>[] paramTypes = null;
        String[] paramNames = null;
        Class<?> returnType = satisfiableResults.getTargetMethod().getReturnType();
        Map<SatisfiableResult, ParsedResult> evalMap = new HashMap<>();
        for (SatisfiableResult res : satisfiableResults.getResults()) {
            if (paramTypes == null) {
                paramTypes = satisfiableResults.getTargetMethod().getParameterTypes();
                paramNames = new String[paramTypes.length];
                int j = 0;
                for (SVarEvaluated key : res.getSymbolicParameterValues()) {
                    // TODO: handle mocked params
                    if (key.getSvar().getType() != VarType.PARAMETER)
                        throw new RuntimeException("MOCKED METHOD INBOUND");
                    paramNames[j] = key.getSvar().getName();
                    j++;
                }
            }

            Object[] parameters = new Object[paramNames.length];
            for (int j = 0; j < paramNames.length; j++)
                parameters[j] = parse(res.getParameter(paramNames[j]), paramTypes[j]);

            Object returnVal = null;
            if (returnType != Void.class)
                returnVal = parse(res.getReturnValue().getEvaluated(), returnType);

            List<SVarEvaluated> parsedFields = new ArrayList<>();
            for (SVarEvaluated sVarEvaluated : res.getSymbolicFieldValues()) {
                Object parsed = parse(sVarEvaluated.getEvaluated(), sVarEvaluated.getSvar().getClassType());
                parsedFields.add(new SVarEvaluated(sVarEvaluated.getSvar(), parsed));
            }

            ParsedResult parsedResult = new ParsedResult(returnVal, parameters, parsedFields);
            evalMap.put(res, parsedResult);
        }
        return evalMap;
    }

    private static <T> T parse(Object value, Class<T> type) {
        if (type == Integer.class || type == int.class)
            return (T) Integer.valueOf(value.toString());

        if (type == String.class) {
            String v = value.toString();
            if (value == null || v.isBlank() || v.length() < 2)
                return (T) value;
            return (T) v.substring(1, v.length()-1);
        }

        if (type == Map.class) {
            if (value == null) return (T) new HashMap<>();
            Map<?,?> map = (Map<?,?>) value;
            Map newMap = new HashMap<>();
            for (Map.Entry<?,?> entry : map.entrySet()) {
                newMap.put(parseString(entry.getKey()), parseString(entry.getValue()));
            }
            return (T) newMap;
        }

        if (value instanceof ClassInstanceVar v)
            return (T) parseObject(v);

        throw new RuntimeException("Could not parse parameter: " + value + " with type: " + type);
    }

    private static <T> T parseObject(ClassInstanceVar<T> instanceVar) {
        Object[] constructorArgs = new Object[instanceVar.getConstructorArgs().length];
        Class[] constructorTypes = instanceVar.getConstructor().getParameterTypes();
        for (int i = 0; i < instanceVar.getConstructorArgs().length; i++) {
            constructorArgs[i] = parse(instanceVar.getConstructorArgs()[i], constructorTypes[i]);
        }

        T object;
        try {
            object = instanceVar.getConstructor().newInstance(instanceVar.getConstructorArgs());
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            for (Field field : instanceVar.getClazz().getDeclaredFields()) {
                String translatedFieldName = "<" + field.getDeclaringClass().getName() + ": " + field.getType().getName() + " " + field.getName() + ">";
                field.setAccessible(true);
                Object value = instanceVar.getFields().getOrDefault(translatedFieldName, null);
                if (value == null) continue;
                field.set(object, parse(value, field.getType()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return object;
    }

    private static <T> T parseString(Object s) {
        if (s instanceof String v && v.length() >= 2) {
            if (v.startsWith("\"") && v.endsWith("\""))
                return (T) v.substring(1, v.length()-1);
        }
        return (T) s;
    }
}
