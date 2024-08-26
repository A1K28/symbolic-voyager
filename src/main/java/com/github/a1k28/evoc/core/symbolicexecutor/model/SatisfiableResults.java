package com.github.a1k28.evoc.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class SatisfiableResults {
    private final List<SatisfiableResult> results;
    private final Method targetMethod;
}
