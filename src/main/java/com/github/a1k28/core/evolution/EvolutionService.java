package com.github.a1k28.core.evolution;

public interface EvolutionService {
    void evolve(
            String classpath,
            String sourceCodePath,
            String sourceClass,
            String targetDirectory) throws Exception;
}
