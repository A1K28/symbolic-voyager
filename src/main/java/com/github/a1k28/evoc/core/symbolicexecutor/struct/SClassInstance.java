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
    private final Map<SNode, Integer> gotoCount; // used for tracking GOTO execution count

    public SClassInstance(Class<?> clazz, CLIOptions cliOptions) {
        this.fields = new HashSet<>();
        this.methodPathSkeletons = new HashMap<>();
        this.clazz = clazz;
        this.cliOptions = cliOptions;
        this.symbolicFieldStack = new SStack();
        this.gotoCount = new HashMap<>();
    }

    public boolean incrementGotoCount(SNode sNode) {
        if (!this.gotoCount.containsKey(sNode))
            this.gotoCount.put(sNode, 0);
        this.gotoCount.put(sNode, this.gotoCount.get(sNode) + 1);
        boolean shouldBreak = this.gotoCount.get(sNode) > 10; // limit is 10
        if (shouldBreak) this.gotoCount.put(sNode, 0);
        return !shouldBreak;
    }

    public void clear() {
        this.gotoCount.clear();
        this.symbolicFieldStack.clear();
    }

    public String getClassname() {
        return this.clazz.getName();
    }
}
