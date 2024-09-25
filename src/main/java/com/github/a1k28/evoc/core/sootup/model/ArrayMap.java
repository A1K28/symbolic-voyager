package com.github.a1k28.evoc.core.sootup.model;

import lombok.Getter;
import lombok.Setter;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;

@Getter
@Setter
public class ArrayMap {
        private ClassType classType;
        private MethodSignature initMethod;
        private MethodSignature addByIndexMethod;
        private MethodSignature getByIndexMethod;
    }