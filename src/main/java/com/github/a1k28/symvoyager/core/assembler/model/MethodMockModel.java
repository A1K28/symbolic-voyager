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
public class MethodMockModel {
    private String type;
    private String methodName;

    private int paramCount;
    private List<Object> parameters;
    private List<String> parameterTypes;
    private List<String> mockType;
    private List<Boolean> shouldDeserializeArgs;
    private List<String> methodMockExtensions;

    private Object retVal;
    private Boolean isStub;
    private String retType;
    private String retExtension;
    private Boolean shouldDeserializeRetVal;
    private String exceptionType;
}
