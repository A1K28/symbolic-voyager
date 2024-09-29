package com.github.a1k28.symvoyager.core.assembler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MockParameter {
    private Object value;
    private String extension;
    private String type;
    private Boolean shouldDeserialize;
    private String mockType;
}
