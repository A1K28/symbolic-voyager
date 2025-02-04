package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.supermock.MockType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.*;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodMockEvaluated;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVarEvaluated;
import com.github.a1k28.symvoyager.helper.Logger;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.github.a1k28.symvoyager.core.sootup.SootInterpreter.translateField;

@NoArgsConstructor
public class SymbolTranslator {
    private static final Logger log = Logger.getInstance(SymbolTranslator.class);

    public static Map<SatisfiableResult, ParsedResult> parse(SatisfiableResults satisfiableResults) {
        Class<?>[] paramTypes = null;
        String[] paramNames = null;
        Class<?> returnType = satisfiableResults.getTargetMethod().getReturnType();
        Map<SatisfiableResult, ParsedResult> evalMap = new HashMap<>();
        Set<ParsedResult> uniqueResultSet = new HashSet<>();
        for (SatisfiableResult res : satisfiableResults.getResults()) {
            if (paramTypes == null) {
                paramTypes = satisfiableResults.getTargetMethod().getParameterTypes();
                paramNames = new String[paramTypes.length];
                int j = 0;
                for (SVarEvaluated key : res.getSymbolicParameterValues()) {
                    paramNames[j] = key.getSvar().getName();
                    j++;
                }
            }

            // params
            Object[] parameters = new Object[paramNames.length];
            for (int j = 0; j < paramNames.length; j++)
                parameters[j] = parse(res.getParameter(paramNames[j]), paramTypes[j]);

            // return val
            Object returnVal = null;
            if (returnType != Void.class
                    && !"void".equals(returnType.toString())
                    && res.getExceptionType() == null) {
                returnVal = parse(res.getReturnValue().getEvaluated(), returnType);
            }

            // fields
            List<SVarEvaluated> parsedFields = new ArrayList<>();
            for (SVarEvaluated sVarEvaluated : res.getSymbolicFieldValues()) {
                Object parsed = parse(sVarEvaluated.getEvaluated(), sVarEvaluated.getSvar().getClassType());
                parsedFields.add(new SVarEvaluated(sVarEvaluated.getSvar(), parsed));
            }

            // method mocks
            List<MethodMockResult> mockedMethodValues = new ArrayList<>();
            Set<String> uniqueMockSet = new HashSet<>();
            for (int k = res.getMockedMethodValues().size()-1; k >= 0; k--) {
                SMethodMockEvaluated sVarEvaluated = res.getMockedMethodValues().get(k);

                // ignore void methods that do not throw exceptions
                if ("void".equals(sVarEvaluated.getMethod().getReturnType().getName())) {
                    if (sVarEvaluated.getExceptionType() == null) continue;
                }

                Class[] mockParamClassTypes = sVarEvaluated.getMethod().getParameterTypes();
                assert mockParamClassTypes.length == sVarEvaluated.getParametersEvaluated().size();
                List<Object> mockParams = new ArrayList<>();
                Object mockRetVal = null;
                Class mockMethodRetType = sVarEvaluated.getMethod().getReturnType();
                Class mockRetType = sVarEvaluated.getSvar().getClassType();
                if (mockMethodRetType.equals(mockRetType) &&
                        !mockMethodRetType.isAssignableFrom(mockRetType)) {
                    mockRetType = mockMethodRetType;
                }
                if (sVarEvaluated.getExceptionType() == null) {
                    mockRetVal = parse(sVarEvaluated.getEvaluated(), mockRetType);
                }
                for (int i = 0; i < mockParamClassTypes.length; i++) {
                    Object v = parse(sVarEvaluated.getParametersEvaluated().get(i), mockParamClassTypes[i]);
                    if (v == null) mockParams.add(MockType.ANY);
                    else mockParams.add(v);
                }

                // only mock stubs once
                boolean isStub = mockRetVal instanceof MockType i && i == MockType.STUB;
                String uniqueKey;
                if (!isStub) uniqueKey = sVarEvaluated.getMethod().toString()
                        + ";"+ Arrays.toString(mockParams.toArray());
                else uniqueKey = mockRetType.getName();
                if (uniqueMockSet.contains(uniqueKey))
                    continue;
                uniqueMockSet.add(uniqueKey);

                MethodMockResult mockResult = new MethodMockResult(
                        sVarEvaluated.getMethod(), mockRetVal, mockRetType, sVarEvaluated.getExceptionType(), mockParams);
                mockedMethodValues.add(mockResult);
            }

            ParsedResult parsedResult = new ParsedResult(
                    returnVal,
                    returnType,
                    parameters,
                    parsedFields,
                    mockedMethodValues,
                    res.getExceptionType());

            // in case the test reduction logic didn't work as intended
            if (uniqueResultSet.contains(parsedResult)) continue;
            uniqueResultSet.add(parsedResult);

            evalMap.put(res, parsedResult);
        }
        return evalMap;
    }

    private static <T> T parse(Object value, Class type) {
        if (value == null)
            return null;

        if (value instanceof ClassInstanceVar v)
            return (T) parseObject(v);

        if (type == Boolean.class || type == boolean.class)
            return (T) Boolean.valueOf(value.toString());
        if (type == Byte.class || type == byte.class)
            return (T) Byte.valueOf(value.toString());
        if (type == Short.class || type == short.class)
            return (T) Short.valueOf(value.toString());
        if (type == Character.class || type == char.class)
            return (T) Character.valueOf(value.toString().charAt(0));
        if (type == Integer.class || type == int.class)
            return (T) Integer.valueOf(value.toString());
        if (type == Long.class || type == long.class)
            return (T) Long.valueOf(value.toString());
        if (type == Float.class || type == float.class)
            return (T) Float.valueOf(value.toString());
        if (type == Double.class || type == double.class)
            return (T) Double.valueOf(value.toString());

        if (type == String.class) {
            String v = value.toString();
            if (value == null || v.isBlank() || v.length() < 2)
                return (T) value;
            return (T) v.substring(1, v.length()-1);
        }

        if (Map.class.isAssignableFrom(type)) {
            if (value == null) return (T) new HashMap<>();
            Map<?,?> map = (Map<?,?>) value;
            Map newMap = new HashMap<>();
            for (Map.Entry<?,?> entry : map.entrySet()) {
                newMap.put(parseString(entry.getKey()), parseString(entry.getValue()));
            }
            return (T) newMap;
        }

        if (List.class.isAssignableFrom(type)) {
            if (value == null) return (T) new ArrayList<>();
            List<?> list = (List<?>) value;
            List newList = new ArrayList();
            for (Object val : list) {
                newList.add(parseString(val));
            }
            return (T) list;
        }

        if (BigInteger.class.isAssignableFrom(type)) {
            return (T) new BigInteger((String) value);
        }

        if (BigDecimal.class.isAssignableFrom(type)) {
            return (T) new BigDecimal((String) value);
        }

        if (type == Class.class) {
            return (T) (((Class) value).getName()+".class");
        }

        throw new RuntimeException("Could not parse parameter: " + value + " with type: " + type);
    }

    private static <T> T parseObject(ClassInstanceVar<T> instanceVar) {
        if (instanceVar.isStub()) {
            return (T) MockType.STUB;
        }

        Object[] constructorArgs = new Object[instanceVar.getConstructorArgs().length];
        Class[] constructorTypes = instanceVar.getConstructor().getParameterTypes();
        for (int i = 0; i < instanceVar.getConstructorArgs().length; i++) {
            constructorArgs[i] = parse(instanceVar.getConstructorArgs()[i], constructorTypes[i]);
        }

        T object;
        try {
            log.trace("Instantiating: " + instanceVar.getConstructor() + " with args: " + Arrays.toString(instanceVar.getConstructorArgs()));
            object = instanceVar.getConstructor().newInstance(instanceVar.getConstructorArgs());
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            for (Field field : instanceVar.getClazz().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = instanceVar.getFields().getOrDefault(translateField(field), null);
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
