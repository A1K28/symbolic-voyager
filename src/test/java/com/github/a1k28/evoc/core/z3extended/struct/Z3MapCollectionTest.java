package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Z3MapCollectionTest {
    //@SymbolicTest({0})
    @DisplayName("test_get_1")
    public int test_get_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");

        if (map.get(a).equals("ASD"))
            return 0;
        return 1;
    }

    //@SymbolicTest({0,1})
    @DisplayName("test_put_overwrite_1")
    public int test_put_overwrite_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "123");
        map.put(a, "ASD");

        map.put("a21412412ed", "123");
        map.put("a2141241awd2ed", "123");
        map.put("a2141awd2412ed", "123");
        map.put("a214awd12412ed", "123");
        map.put("a21412awd412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");

        if (map.get(a).equals("123"))
            return 0;
        if (map.get(a).equals("ASD"))
            return 1;
        if (map.get("asdadawd").equals("12e"))
            return 2;
        if (map.get("awd").equals("awdawd"))
            return 3;
        if (map.get("12441").equals("124124"))
            return 4;
        if (map.get("adwawdadw").equals("awdawd"))
            return 5;
        if (map.get("asdsddd").equals("asas"))
            return 6;
        if (map.get("awdawd").equals("awadw"))
            return 7;
        if (map.get("sadwad").equals("adwdaw"))
            return 8;
        if (map.get("asdawdawda").equals("awdawd"))
            return 9;
        return 2;
    }

    //@SymbolicTest({0,1})
    @DisplayName("test_size_1")
    public int test_size_1(String a, String b) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        map.put(b, "123");

        if (map.size() == 2)
            return 0;
        return 1;
    }

    //@SymbolicTest({0,1})
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

    //@SymbolicTest({6,7})
    @DisplayName("test_size_3")
    public int test_size_3(String a, String b) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        if (a.equals("RANDOM TEXT"))
            map.put(b, "123");

        map.put("a21412412ed", "123");
        map.put("a2141241awd2ed", "123");
        map.put("a2141awd2412ed", "123");
        map.put("a214awd12412ed", "123");
        map.put("a21412awd412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");

        if (map.size() == 2)
            return 0;
        if (map.size() == 3)
            return 1;
        if (map.size() == 4)
            return 2;
        if (map.size() == 5)
            return 3;
        if (map.size() == 6)
            return 4;
        if (map.size() == 7)
            return 5;
        if (map.size() == 8)
            return 6;
        return 7;
    }

    //@SymbolicTest({0,1})
    @DisplayName("test_size_with_input_1")
    public int test_size_with_input_1(Map<String, String> map) {
        if (map.size() == 3)
            return 0;
        return 1;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_size_with_input_2")
    public int test_size_with_input_2(Map<String, String> map, String a) {
        if (map.size() == 1) {
            map.put(a, "ASD");
            if (a.equals("ASDFGH123"))
                return 0;
            if (map.size() == 2)
                return 1;
        }
        return 2;
    }

    //@SymbolicTest({0,1})
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

    //@SymbolicTest({0,1})
    @DisplayName("test_contains_key_with_input_1")
    public int test_contains_key_with_input_1(Map<String, String> map) {
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    //@SymbolicTest({0,1})
    @DisplayName("test_contains_key_with_input_2")
    public int test_contains_key_with_input_2(Map<String, String> map) {
        if (map.containsKey("KEY1") && map.containsKey("KEY2"))
            return 0;
        return 1;
    }

    private void fillValues(Map map) {
        map.put("a21412412ed", "123");
        map.put("a2141241awd2ed", "123");
        map.put("a2141awd2412ed", "123");
        map.put("a214awd12412ed", "123");
        map.put("a21412awd412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a21412asd412ed", "123");
        map.put("a214asd12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
        map.put("a214zxc12412ed", "123");
    }
}