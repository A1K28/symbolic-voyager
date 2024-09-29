package com.github.a1k28.symvoyager.core.assembler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Field {
    private String name;
    private String nameCapitalized;
    private Object value;
    private String extension;
    private String type;
    private Boolean shouldDeserialize;
    private Boolean isStatic;
    private Boolean methodExists;
}
