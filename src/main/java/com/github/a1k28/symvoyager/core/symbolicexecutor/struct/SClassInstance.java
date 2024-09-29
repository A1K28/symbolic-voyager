package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import lombok.Getter;
import lombok.Setter;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class SClassInstance {
    private final Class<?> clazz;
    private final SootClass<JavaSootClassSource> sootClass;
    private final Set<String> fieldNames;
    private final List<JavaSootField> fields;
    private final SStack symbolicFieldStack;
    private final Map<Executable, SMethodPath> methodPathSkeletons;
    private final Map<SNode, Integer> gotoCount; // used for tracking GOTO execution count

    public SClassInstance(Class<?> clazz) {
        this.clazz = clazz;
        this.sootClass = null;
        this.fields = null;
        this.fieldNames = null;
        this.methodPathSkeletons = null;
        this.symbolicFieldStack = null;
        this.gotoCount = null;
    }

    public SClassInstance(Class<?> clazz,
                          SootClass<JavaSootClassSource> sootClass,
                          List<JavaSootField> fields) {
        this.clazz = clazz;
        this.sootClass = sootClass;
        this.fields = fields;
        this.fieldNames = fields.stream().map(SootClassMember::toString).collect(Collectors.toSet());
        this.methodPathSkeletons = new HashMap<>();
        this.symbolicFieldStack = new SStack();
        this.gotoCount = new HashMap<>();
    }

    public boolean incrementGotoCount(SNode sNode) {
        if (!this.gotoCount.containsKey(sNode))
            this.gotoCount.put(sNode, 0);
        this.gotoCount.put(sNode, this.gotoCount.get(sNode) + 1);
        boolean shouldBreak = this.gotoCount.get(sNode) > CLIOptions.gotoLimit;
//        if (shouldBreak) this.gotoCount.put(sNode, 0);
        return !shouldBreak;
    }

//    public void clear() {
//        this.gotoCount.clear();
//        this.symbolicFieldStack.clear();
//    }
//
//    public String getClassname() {
//        return this.clazz.getName();
//    }
}
