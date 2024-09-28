package com.github.a1k28.symvoyager.core.assembler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MethodCallModel {
    private String testName;
    private String methodName;

    private String returnType;
    private Object returnValue;
    private Boolean shouldDeserializeRetVal;
    private String exceptionType;

    private int paramCount;
    private List<Object> parameters;
    private List<String> parameterTypes;
    private List<Boolean> shouldDeserializeArgs;

    private int mockCount;
    private List<MethodMockModel> methodMocks;
}
