package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.EvaluatedResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class SymbolTranslator {
    public static Map<SatisfiableResult, EvaluatedResult> translate(SatisfiableResults satisfiableResults) {
        Class<?>[] paramTypes = null;
        String[] paramNames = null;
        Class<?> returnType = satisfiableResults.getTargetMethod().getReturnType();
        Map<SatisfiableResult, EvaluatedResult> evalMap = new HashMap<>();
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
            EvaluatedResult evaluatedResult = new EvaluatedResult(returnVal, parameters);
            evalMap.put(res, evaluatedResult);
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

        throw new RuntimeException("Could not parse parameter: " + value + " with type: " + type);
    }

    private static <T> T parseString(Object s) {
        if (s instanceof String v && v.length() >= 2) {
            if (v.startsWith("\"") && v.endsWith("\""))
                return (T) v.substring(1, v.length()-1);
        }
        return (T) s;
    }
}
