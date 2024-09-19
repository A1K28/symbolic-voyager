package com.github.a1k28.evoc.core.assembler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassModel {
    private String packageName;
    private String className;
    private List<String> imports;
    private List<MethodCallModel> methodCallModels;
}
