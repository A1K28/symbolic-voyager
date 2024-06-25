package com.github.a1k28.evoc.core.mutator;

import com.github.a1k28.evoc.model.mutator.DetectionMatrix;

public interface MutationService {
    DetectionMatrix generateMutants(String targetClass, String targetTestClass);
}
