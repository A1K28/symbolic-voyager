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
    private String exceptionType;

    private Fields fields;
    private Parameter retVal;
    private Parameters parameters;

    private int mockCount;
    private List<MethodMockModel> methodMocks;
}
