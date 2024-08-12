package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Z3MapCollectionTest {
    @SymbolicTest({0,1})
    @DisplayName("test_put_1")
    public int test_put_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put("123", "ASD");
        map.put(a, "ASD");
        if (map.containsKey("TEEEST")) {
            System.out.println("123123123");
            return 0;
        }
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_put_with_input_1")
    public int test_put_with_input_1(Map<String, String> map) {
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }
}