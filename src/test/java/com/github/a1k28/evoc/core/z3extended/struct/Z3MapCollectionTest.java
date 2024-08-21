package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Z3MapCollectionTest {
    @SymbolicTest({0})
    @DisplayName("test_get_1")
    public int test_get_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");

        if (map.get(a).equals("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0})
    @DisplayName("test_put_overwrite_1")
    public int test_put_overwrite_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        map.put(a, "123");

        if (map.get(a).equals("123"))
            return 0;
        if (map.get(a).equals("ASD"))
            return 1;
        return 2;
    }

    @SymbolicTest({0})
    @DisplayName("test_size_1")
    public int test_size_1(String a, String b) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        map.put(b, "123");

        if (map.size() == 2)
            return 0;
        return 1;
    }

    @SymbolicTest({0, 1})
    @DisplayName("test_size_2")
    public int test_size_2(String a, String b) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        if (a.equals("RANDOM TEXT"))
            map.put(b, "123");

        if (map.size() == 2)
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_1")
    public int test_contains_key_1(String a) {
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
    @DisplayName("test_contains_key_with_input_1")
    public int test_contains_key_with_input_1(Map<String, String> map) {
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_with_input_2")
    public int test_contains_key_with_input_2(Map<String, String> map) {
        if (map.containsKey("KEY1") && map.containsKey("KEY2"))
            return 0;
        return 1;
    }
}