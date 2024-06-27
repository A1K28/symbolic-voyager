package com.github.a1k28.evoc.model.branch;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class BranchDistanceData {
    private final String testId;
    private final Map<String, Float> distances;

    public BranchDistanceData(String testId) {
        this.testId = testId;
        this.distances = new HashMap<>();
    }
}
