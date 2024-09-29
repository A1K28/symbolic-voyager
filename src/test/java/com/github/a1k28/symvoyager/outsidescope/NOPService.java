package com.github.a1k28.symvoyager.outsidescope;

import com.github.a1k28.symvoyager.core.z3extended.model.StatusDTO;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class NOPService {
    private DependentService dependentService;

    public static <T> T getInstance(DependentService dependentService) {
        return (T) new NOPService();
    }

    public static API getInstance() {
        return null;
    }

    public StatusDTO getStatus(String asd) {
        return new StatusDTO();
    }

    // method body does not matter since this method should be mocked
    public Integer calculate() {
        return 20;
    }

    public int calculate(int a) {
        System.out.println("ASD");
        return a*2;
    }

    public void calculate2(int a) {
        System.out.println("ASD");
    }

    public Integer calculate(Integer a, Integer b) {
        return a * b;
    }

    public boolean diffuse(int k) {
        return dependentService.diffuse(k);
    }
}
