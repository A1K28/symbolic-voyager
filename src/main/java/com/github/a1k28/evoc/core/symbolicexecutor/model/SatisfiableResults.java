package com.github.a1k28.evoc.core.symbolicexecutor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SatisfiableResults {
    private Integer coveredLines = 0;
    private final Integer totalLines;
    private final List<SatisfiableResult> results;

    public void incrementCoveredLines() {
        this.coveredLines += 1;
    }
}
