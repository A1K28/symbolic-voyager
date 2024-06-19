package com.github.a1k28.core.mutator;

public interface MutationService {
    DetectionMatrix generateMutants(String targetClass, String targetTestClass);
}
