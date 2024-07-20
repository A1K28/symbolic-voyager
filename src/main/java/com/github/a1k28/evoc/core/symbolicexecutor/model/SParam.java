package com.github.a1k28.evoc.core.symbolicexecutor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class SParam {
    private final Object o;

    public SParam() {
        this.o = null;
    }
}
