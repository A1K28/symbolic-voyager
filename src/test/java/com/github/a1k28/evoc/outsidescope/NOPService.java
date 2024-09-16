package com.github.a1k28.evoc.outsidescope;

public class NOPService {
    // method body does not matter since this method should be mocked
    public Integer calculate() {
        return 20;
    }

    public void calculate(int a) {
        System.out.println("ASD");
    }

    public Integer calculate(Integer a, Integer b) {
        return a * b;
    }
}
