package com.github.a1k28.core.mutator;

import com.github.a1k28.model.mutator.DetectionMatrix;

public interface MutationService {
    DetectionMatrix generateMutants(String targetClass, String targetTestClass);
}
