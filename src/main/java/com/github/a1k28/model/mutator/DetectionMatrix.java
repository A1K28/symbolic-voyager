package com.github.a1k28.model.mutator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetectionMatrix {
    private String[] tests;
    private String[] mutants;
    private boolean[][] matrix;
}