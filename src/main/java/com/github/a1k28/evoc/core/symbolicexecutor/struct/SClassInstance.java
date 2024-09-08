package com.github.a1k28.evoc.core.symbolicexecutor.struct;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import lombok.Getter;
import lombok.Setter;
import sootup.core.model.SootClassMember;
import sootup.java.core.JavaSootField;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class SClassInstance {
    private final Class<?> clazz;
    private final Set<String> fieldNames;
    private final List<JavaSootField> fields;
    private final SStack symbolicFieldStack;
    private final Map<Executable, SMethodPath> methodPathSkeletons;
    private final CLIOptions cliOptions;
    private final Map<SNode, Integer> gotoCount; // used for tracking GOTO execution count
    private boolean isUnknown = true;

    public SClassInstance(Class<?> clazz, List<JavaSootField> fields, CLIOptions cliOptions) {
        this.clazz = clazz;
        this.fields = fields;
        this.cliOptions = cliOptions;
        this.fieldNames = fields.stream().map(SootClassMember::toString).collect(Collectors.toSet());
        this.methodPathSkeletons = new HashMap<>();
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
