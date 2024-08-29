package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//get
//put
//size
//isEmpty
//containsKey
//containsValue
//remove


public class Z3MapCollectionTest {
    @SymbolicTest({0,1})
    @DisplayName("test_remove_1")
    public int test_remove_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put("AAWDAWD", "ASD");
        map.remove(a);
        if (map.containsValue("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_remove_winput_1")
    public int test_remove_winput_1(Map map, String a) {
        map.put("AAWDAWD", "ASD");
        map.remove(a);
        if (map.containsValue("AAWDAWD"))
            return 0;
        return 1;
    }


    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_1")
    public int test_contains_key_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put("123", "ASD");
        map.put(a, "ASD");
        if (map.containsKey("TEEEST"))
            return 0;
        if (map.containsKey("123"))
            return 1;
        return 2;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_winput_1")
    public int test_contains_key_winput_1(Map<String, String> map) {
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_winput_2")
    public int test_contains_key_winput_2(Map<String, String> map) {
        map.containsKey("AWDAD");
        if (map.containsKey("KEY1") && map.containsKey("KEY2"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_winput_3")
    public int test_contains_key_winput_3(Map<String, String> map) {
        map.put("123", "adwawdd");
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_winput_4")
    public int test_contains_key_winput_4(Map<String, String> map, String a) {
        map.put(a, "adwawdd");
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1,2,3,5})
    @DisplayName("test_contains_key_winput_5")
    public int test_contains_key_winput_5(Map map, String a) {
        if (map.containsKey("VALUE1"))
            return 0;
        if (map.containsKey(a))
            return 1;
        if (map.isEmpty())
            return 2;
        map.put("KEY2", "VALUE2");
        if (map.containsKey(a))
            return 3;
        map.remove("KEY3");
        if (map.containsKey("KEY3"))
            return 4;
        return 5;
    }

    @SymbolicTest({1,2})
    @DisplayName("test_contains_value_1")
    public int test_contains_value_1(String a) {
        Map<String, String> map = new HashMap<>();
        if (map.containsValue(a))
            return 0;
        map.put("AAWDAWD", "ASD");
        if (map.containsValue(a))
            return 1;
        if (map.containsValue("ASD"))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_contains_value_winput_1")
    public int test_contains_value_winput_1(Map map, String a) {
        if (map.containsValue("VALUE1"))
            return 0;
        map.put("KEY2", "VALUE2");
        if (map.containsValue(a))
            return 1;
        return 2;
    }

    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_contains_value_winput_2")
    public int test_contains_value_winput_2(Map map, String a) {
        if (map.containsValue("VALUE1"))
            return 0;
        if (map.containsValue(a))
            return 1;
        map.put("KEY2", "VALUE2");
        if (map.containsValue(a))
            return 2;
        map.remove("KEY2");
        if (map.containsValue("VALUE2"))
            return 3;
        return 4;
    }

    @SymbolicTest({0})
    @DisplayName("test_is_empty_1")
    public int test_is_empty_1(String a) {
        Map<String, String> map = new HashMap<>();
        if (map.isEmpty())
            return 0;
        map.put(a, "ASD");
        if (map.isEmpty())
            return 1;
        return 2;
    }

    @SymbolicTest({1,2})
    @DisplayName("test_is_empty_2")
    public int test_is_empty_2(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        if (map.isEmpty())
            return 0;
        map.remove("AWDAWDAW");
        if (map.isEmpty())
            return 1;
        return 2;
    }

    @SymbolicTest({0,2})
    @DisplayName("test_is_empty_winput_1")
    public int test_is_empty_winput_1(Map map, String a) {
        if (map.isEmpty())
            return 0;
        map.put(a, "ASD");
        if (map.isEmpty())
            return 1;
        return 2;
    }

    @SymbolicTest({0})
    @DisplayName("test_get_1")
    public int test_get_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");

        if (map.get(a).equals("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
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

    @SymbolicTest({0,1})
    @DisplayName("test_size_1")
    public int test_size_1(String a, String b) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "ASD");
        map.put(b, "123");

        if (map.size() == 2)
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
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

    @SymbolicTest({6,7})
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

    @SymbolicTest({0,1})
    @DisplayName("test_size_winput_1")
    public int test_size_winput_1(Map<String, String> map) {
        if (map.size() == 3)
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_size_winput_2")
    public int test_size_winput_2(Map<String, String> map, String a) {
        map.put(a, "ASD");
        if (map.size() == 3)
            return 0;
        return 1;
    }

    @SymbolicTest({0,1,2,3})
    @DisplayName("test_size_winput_3")
    public int test_size_winput_3(Map<String, String> map, String a) {
        if (map.size() == 1) {
            map.put(a, "ASD");
            if (a.equals("ASDFGH123"))
                return 0;
            if (map.size() == 1)
                return 1;
            if (map.size() == 2)
                return 2;
        }
        return 3;
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