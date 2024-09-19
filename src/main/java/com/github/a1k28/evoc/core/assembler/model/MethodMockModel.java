package com.github.a1k28.evoc.core.assembler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MethodMockModel {
    private String type;
    private String methodName;
    private Object[] args;
    private Object retVal;
    private Class exceptionType;
}
