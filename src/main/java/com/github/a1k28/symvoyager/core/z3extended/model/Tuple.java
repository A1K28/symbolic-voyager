package com.github.a1k28.symvoyager.core.z3extended.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tuple<T> {
    private T o1;
    private T o2;
}
