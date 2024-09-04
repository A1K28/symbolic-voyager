package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

public class Z3MapTest {
    @SymbolicTest({0})
    @DisplayName("test_copy_of_1")
    public int test_copy_of_1(String a) {
        Map map2 = new HashMap();
        map2.put("ASDASD", "ASDASD");
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        map2 = Map.copyOf(map1);
        if (map1.equals(map2))
            return 0;
        return 1;
    }

    @SymbolicTest({0})
    @DisplayName("test_copy_of_winput_1")
    public int test_copy_of_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        Map map2 = Map.copyOf(map1);
        if (map1.equals(map2))
            return 0;
        return 1;
    }

    @SymbolicTest({0})
    @DisplayName("test_copy_of_winput_2")
    public int test_copy_of_winput_2(Map map1, Map map2) {
        map2.put("ASDASDASDAD", "ASDAWDAW");
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map2 = Map.copyOf(map1);
        if (map1.equals(map2))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_replace_1")
    public int test_replace_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        map1.replace("KEY1", "VALUE3");
        if (map1.containsKey(a))
            return 0;
        if (map1.containsValue("VALUE3"))
            return 1;
        return 2;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_replace_winput_1")
    public int test_replace_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        map1.replace("KEY1", "VALUE3");
        if (map1.containsKey(a))
            return 0;
        if (map1.containsValue("VALUE3"))
            return 1;
        return 2;
    }

    @SymbolicTest({0,4,8})
    @DisplayName("test_replace_by_key_and_value_1")
    public int test_replace_by_key_and_value_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        if (map1.replace("KEY1", "VALUE1", "VALUE3"))
            return 0;
        if (!map1.replace("KEY2", "VALUE2", "VALUE4"))
            return 1;
        if (!map1.replace("KEY1", a, a+"ASD"))
            return 2;
        if (map1.containsValue("VALUE2"))
            return 3;
        if (map1.containsValue(a))
            return 4;
        if (!map1.containsValue(a+"ASD"))
            return 5;
        if (!map1.containsValue("VALUE4"))
            return 6;
        if (map1.containsValue("VALUE3"))
            return 7;
        return 8;
    }

    @SymbolicTest({0,1,4,5,6,7})
    @DisplayName("test_replace_by_key_and_value_winput_1")
    public int test_replace_by_key_and_value_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        if (map1.replace("KEY1", "VALUE1", "VALUE3"))
            return 0;
        if (map1.replace("KEY1", "VALUE3", a+"ASD"))
            return 1;
        if (!map1.containsValue("VALUE2"))
            return 2;
        if (!map1.containsValue(a))
            return 3;
        if (!map1.containsValue(a+"ASD"))
            return 4;
        if (!map1.containsValue("VALUE4"))
            return 5;
        if (map1.containsValue("VALUE3"))
            return 6;
        return 7;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_remove_by_key_and_value_1")
    public int test_remove_by_key_and_value_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        if (map1.remove("KEY1", "VALUE1"))
            return 0;
        map1.remove("KEY1", a);
        if (map1.remove("KEY2", a))
            return 1;
        if (map1.remove("KEY2", "VALUE2"))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_remove_by_key_and_value_winput_1")
    public int test_remove_by_key_and_value_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        map1.put("KEY2", "VALUE2");
        if (map1.remove("KEY1", "VALUE1"))
            return 0;
        map1.remove("KEY1", a);
        if (map1.remove("KEY2", a))
            return 1;
        if (map1.remove("KEY2", "VALUE2"))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_put_if_absent_1")
    public int test_put_if_absent_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        String prev = map1.putIfAbsent("KEY1", "VALUE2");
        if ("NULL".equals(a))
            return 0;
        if ("KEY1".equals(prev))
            return 1;
        if (a.equals(map1.getOrDefault("KEY1", "NULL")))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_put_if_absent_winput_1")
    public int test_put_if_absent_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        String prev = (String) map1.putIfAbsent("KEY1", "VALUE2");
        if ("NULL".equals(a))
            return 0;
        if ("KEY1".equals(prev))
            return 1;
        if (a.equals(map1.getOrDefault("KEY1", "NULL")))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2,3})
    @DisplayName("test_get_or_default_1")
    public int test_get_or_default_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", a);
        if ("NULL".equals(map1.get("KEY1")))
            return 0;
        if ("ASD".equals(map1.getOrDefault("KEY1", "123")))
            return 1;
        if ("VALUE1".equals(map1.getOrDefault("KEY1", "123")))
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_get_or_default_winput_1")
    public int test_get_or_default_winput_1(Map map1, String a) {
        map1.put("KEY1", a);
        if ("NULL".equals(map1.get("KEY1")))
            return 0;
        if ("ASD".equals(map1.getOrDefault("KEY1", "123")))
            return 1;
        if ("VALUE1".equals(map1.getOrDefault("KEY1", "123")))
            return 2;
        if ("VALUE2".equals(map1.getOrDefault("KEY1", a)))
            return 3;
        return 4;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_equals_1")
    public int test_equals_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        Map<String, String> map2 = new HashMap<>();
        map2.put("KEY1", "VALUE1");
        map2.put("KEY2", "VALUE2");
        map2.put("KEY3", "VALUE3");
        if (map1.equals(map2))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_equals_winput_1")
    public int test_equals_winput_1(Map map1, Map map2, String a) {
        map1.put(a, "VALUE3");
        map2.put("KEY1", "VALUE1");
        map2.put("KEY2", "VALUE2");
        map2.put("KEY3", "VALUE3");
        if (map1.equals(map2))
            return 0;
        return 1;
    }

    @SymbolicTest({0,3})
    @DisplayName("test_clear_1")
    public int test_clear_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        if (map1.size() == 3)
            return 0;
        map1.clear();
        if (map1.size() > 0)
            return 1;
        if (map1.containsKey("KEY1"))
            return 2;
        return 3;
    }

    @SymbolicTest({0,2,3})
    @DisplayName("test_clear_2")
    public int test_clear_2(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        if (map1.size() == 3)
            return 0;
        map1.clear();
        if (map1.size() > 0)
            return 1;
        map1.put(a, "VALUE4");
        if (map1.containsKey("KEY1"))
            return 2;
        if (map1.containsValue("VALUE4"))
            return 3;
        return 4;
    }

    @SymbolicTest({0,2,3})
    @DisplayName("test_clear_winput_1")
    public int test_clear_winput_1(Map map1, String a) {
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        if (map1.size() == 3)
            return 0;
        map1.clear();
        if (map1.size() > 0)
            return 1;
        map1.put(a, "VALUE4");
        if (map1.containsKey("KEY1"))
            return 2;
        if (map1.containsValue("VALUE4"))
            return 3;
        return 4;
    }

    @SymbolicTest({2,3,4})
    @DisplayName("test_init_from_map_1")
    public int test_init_from_map_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        Map<String, String> map2 = new HashMap<>(map1);
        if (map2.isEmpty())
            return 0;
        if (map2.size() == 1)
            return 1;
        if (map2.containsKey("KEY3"))
            return 2;
        if (map2.size() == 3)
            return 3;
        return 4;
    }

    @SymbolicTest({2,3,4})
    @DisplayName("test_put_all_TKSK_1")
    public int test_put_all_TKSK_1(String a) {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put(a, "VALUE3");
        map2.putAll(map1);
        if (map2.isEmpty())
            return 0;
        if (map2.size() == 1)
            return 1;
        if (map2.containsKey("KEY3"))
            return 2;
        if (map2.size() == 3)
            return 3;
        return 4;
    }

    @SymbolicTest({1,2,3,4})
    @DisplayName("test_put_all_TKSU_winput_1")
    public int test_put_all_TKSU_winput_1(Map map1, String a) {
        Map<String, String> map2 = new HashMap<>();
        map2.put(a, "VALUE1");
        map2.putAll(map1);
        if (map2.isEmpty())
            return 0;
        if (map2.size() == 1)
            return 1;
        if (map2.containsKey("KEY3"))
            return 2;
        if (map2.size() == 3)
            return 3;
        return 4;
    }

    @SymbolicTest({0,2,3,4})
    @DisplayName("test_put_all_TUSK_winput_1")
    public int test_put_all_TUSK_winput_1(Map map1, String a) {
        Map<String, String> map2 = new HashMap<>();
        map2.put("KEY1", "VALUE1");
        map2.put(a, a);
        map1.putAll(map2);
        if (map1.size() > 5 || map1.isEmpty())
            return 0;
        if (map2.isEmpty())
            return 1;
        if (map2.size() == 2)
            return 2;
        if (map1.size() == 2)
            return 3;
        if (map1.containsKey(a))
            return 4;
        if (map1.containsValue("VALUE1"))
            return 5;
        return 6;
    }


    @SymbolicTest({0,1,3,4,5,6})
    @DisplayName("test_put_all_TUSU_winput_1")
    public int test_put_all_TUSU_winput_1(Map map1, Map map2) {
        map2.putAll(map1);
        if (map2.isEmpty() && map1.isEmpty())
            return 0;
        if (map1.isEmpty())
            return 1;
        if (map2.isEmpty())
            return 2;
        if (map2.size() == 1)
            return 3;
        if (map2.containsKey("KEY3"))
            return 4;
        if (map2.size() == 3)
            return 5;
        return 6;
    }

    @SymbolicTest({3,4,5,6})
    @DisplayName("test_put_all_TUSU_winput_2")
    public int test_put_all_TUSU_winput_2(Map map1, Map map2, String a) {
        map1.put(a, a);
        map2.putAll(map1);
        if (map2.isEmpty() && map1.isEmpty())
            return 0;
        if (map1.isEmpty())
            return 1;
        if (map2.isEmpty())
            return 2;
        if (map2.size() == 1)
            return 3;
        if (map1.containsKey("ASD"))
            return 4;
        if (map2.size() == 3)
            return 5;
        return 6;
    }

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
    public int test_contains_key_winput_4(Map map, String a) {
        map.put(a, "adwawdd");
        if (map.containsKey("ASD"))
            return 0;
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_contains_key_winput_4_2")
    public int test_contains_key_winput_4_2(String a, Map map) {
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

    @SymbolicTest({0,1,2})
    @DisplayName("test_put_winput_1")
    public int test_put_winput_1(Map map, String a) {
        map.put(a, "VALUE0");
        map.put("KEY1", "VALUE1");
        map.put("KEY2", "VALUE2");
        map.put("KEY2", "VALUE3");
        if (map.size() == 3)
            return 0;
        if (map.size() == 5)
            return 1;
        return 2;
    }

    @SymbolicTest({2,3})
    @DisplayName("test_put_winput_2")
    public int test_put_winput_2(Map map1) {
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put("KEY3", "VALUE3");
        if (map1.isEmpty())
            return 0;
        if (map1.size() < 3)
            return 1;
        if (map1.size() < 10)
            return 2;
        return 3;
    }

    @SymbolicTest({1,2,3})
    @DisplayName("test_put_winput_3")
    public int test_put_winput_3(Map map1) {
        map1.put("KEY1", "VALUE1");
        map1.put("KEY2", "VALUE2");
        map1.put("KEY2", "VALUE3");
        if (map1.isEmpty())
            return 0;
        if (map1.size() < 3)
            return 1;
        if (map1.size() < 10)
            return 2;
        return 3;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_put_overwrite_1")
    public int test_put_overwrite_1(String a) {
        Map<String, String> map = new HashMap<>();
        map.put(a, "123");
        map.put(a, "ASD");

        fillValues(map);

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

        fillValues(map);

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

    @SymbolicTest({1,2,3,4})
    @DisplayName("test_size_winput_4")
    public int test_size_winput_4(Map map1, Map map2, String a) {
        map1.put(a, "ASD");
        if (map1.size() == 0)
            return 0;
        if (map2.size() == 0)
            return 1;
        if (map1.size() == 3)
            return 2;
        if (map2.size() == 10)
            return 3;
        return 4;
    }

    @SymbolicTest({2})
    @DisplayName("test_map_of_1")
    public int test_map_of_1() {
        Map map = Map.of();
        if (!map.isEmpty())
            return 0;
        if (map.size() > 0)
            return 1;
        return 2;
    }

    @SymbolicTest({3,5,7})
    @DisplayName("test_map_of_2")
    public int test_map_of_2(String a) {
        Map map = Map.of("KEY1", "VALUE1");
        if (map.isEmpty())
            return 0;
        if (map.size() != 1)
            return 1;
        if (!map.containsKey("KEY1"))
            return 2;
        if (map.containsKey(a))
            return 3;
        if (map.containsKey("KEY2"))
            return 4;
        if (map.containsValue(a))
            return 5;
        if (!map.containsValue("VALUE1"))
            return 6;
        return 7;
    }

    @SymbolicTest({3,5,9})
    @DisplayName("test_map_of_3")
    public int test_map_of_3(String a) {
        Map map = Map.of("KEY1", "VALUE1", "KEY2", "VALUE2");
        if (map.isEmpty())
            return 0;
        if (map.size() != 2)
            return 1;
        if (!map.containsKey("KEY1"))
            return 2;
        if (map.containsKey(a))
            return 3;
        if (!map.containsKey("KEY2"))
            return 4;
        if (map.containsValue(a))
            return 5;
        if (!map.containsValue("VALUE1"))
            return 6;
        if (!map.containsValue("VALUE2"))
            return 7;
        if (map.containsValue("VALUE3"))
            return 8;
        return 9;
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