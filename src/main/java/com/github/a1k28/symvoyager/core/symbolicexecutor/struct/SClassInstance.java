package com.github.a1k28.symvoyager.core.symbolicexecutor.struct;

import lombok.Getter;
import lombok.Setter;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
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
    private final SootClass sootClass;
    private final Set<String> fieldNames;
    private final List<JavaSootField> fields;
    private final SStack symbolicFieldStack;
    private final Map<Executable, SMethodPath> methodPathSkeletons;

    public SClassInstance(Class<?> clazz) {
        this.clazz = clazz;
        this.sootClass = null;
        this.fields = null;
        this.fieldNames = null;
        this.methodPathSkeletons = null;
        this.symbolicFieldStack = null;
    }

    public SClassInstance(Class<?> clazz,
                          SootClass sootClass,
                          List<JavaSootField> fields) {
        this.clazz = clazz;
        this.sootClass = sootClass;
        this.fields = fields;
        this.fieldNames = fields.stream().map(SootClassMember::toString).collect(Collectors.toSet());
        this.methodPathSkeletons = new HashMap<>();
        this.symbolicFieldStack = new SStack();
    }
}
