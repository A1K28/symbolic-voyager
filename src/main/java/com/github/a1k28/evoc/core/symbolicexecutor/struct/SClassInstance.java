package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class SClassInstance {
    private final Class<?> clazz;
    private final Set<String> fields;
    private final SStack symbolicFieldStack;
    private final Map<Method, SMethodPath> methodPathSkeletons;
    private final CLIOptions cliOptions;

    public SClassInstance(Class<?> clazz, CLIOptions cliOptions) {
        this.fields = new HashSet<>();
        this.methodPathSkeletons = new HashMap<>();
        this.clazz = clazz;
        this.cliOptions = cliOptions;
        this.symbolicFieldStack = new SStack();
    }

    public String getClassname() {
        return this.clazz.getName();
    }
}
