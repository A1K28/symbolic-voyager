package com.github.a1k28.symvoyager.outsidescope;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class NOPService {
    private DependentService dependentService;

    public static <T> T getInstance(Boolean dependentService) {
        return (T) new NOPService();
    }
    public static API getInstance() {
        return null;
    }

    // method body does not matter since this method should be mocked
    public Integer calculate() {
        return 20;
    }

    public int calculate(int a) {
        System.out.println("ASD");
        return a*2;
    }

    public Integer calculate(Integer a, Integer b) {
        return a * b;
    }

    public boolean diffuse(int k) {
        return dependentService.diffuse(k);
    }
}
