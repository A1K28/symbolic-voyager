package com.github.a1k28.model.evolution;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MutationTestInfo {
    private float score;
    private List<String> testsToKeep;
    private List<String> parentTests;
}
