package com.github.a1k28.evoc.core.z3extended.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerInfoDTO {
    private StatusDTO status;
    private String customerId;
    private Integer debt;

    public CustomerInfoDTO(String customerId) {
        this.customerId = customerId;
    }

    public static void printASD() {
        System.out.println("ASD");
    }
}
