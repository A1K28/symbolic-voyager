package com.github.a1k28.symvoyager.core.assembler.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {
    private Object value;
    private String extension;
    private String type;
    private Boolean shouldDeserialize;
}
