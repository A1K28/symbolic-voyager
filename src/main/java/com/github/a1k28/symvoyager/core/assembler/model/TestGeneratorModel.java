package com.github.a1k28.symvoyager.core.assembler.model;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.ParsedResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Method;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestGeneratorModel {
    private Method method;
    private ParsedResult parsedResult;
}
