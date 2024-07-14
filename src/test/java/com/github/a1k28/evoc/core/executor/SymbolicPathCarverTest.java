package com.github.a1k28.evoc.core.executor;

import com.github.a1k28.evoc.core.executor.struct.SVar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SymbolicPathCarverTest {
    private SymbolicPathCarver symbolicPathCarver;

    private int asd;

    @BeforeEach
    public void setup() {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
        symbolicPathCarver = new SymbolicPathCarver(
                "com.github.a1k28.evoc.core.executor.SymbolicPathCarverTest");
    }

    @Test
    public void testMethodCall() throws ClassNotFoundException {
        List<Map<SVar, String>> res = symbolicPathCarver
                .analyzeSymbolicPaths( "test_inner_method_calls");

        List<Map<String, Object>> solutionSet = new ArrayList<>();
        solutionSet.add(Map.of("a", 0, "b", 0, "asd", "17"));
        solutionSet.add(Map.of("a", 10, "b", 0, "asd", "17"));

       outer: for (Map<SVar, String> map : res) {
            Map<String, String> named = mapSVars(map);
            for (Map<String, Object> sol : solutionSet) {
                if (!sol.get("a").equals(named.get("a").length())) continue;
                if (!sol.get("b").equals(named.get("b").length())) continue;
                if (!sol.get("asd").equals(named.get("asd"))) continue;
                continue outer;
            }
            assertTrue(false);
        }
    }

    private String test_inner_method_calls(String a, String b) {
        a += "c";
        int l = int_test(a);
        if (l < asd) return b;
        return a;
    }

    private int int_test(String a) {
//        return 20;
        a = "123" + str_test(a);
        return a.length();
    }

    private String str_test(String a) {
        asd = 17;
        return a + "IOP";
    }

    private Map<String, String> mapSVars(Map<SVar, String> map) {
        Map<String, String> res = new HashMap<>();
        for (Map.Entry<SVar, String> entry : map.entrySet()) {
            String name = entry.getKey().getName();
            name = name.replace("<", "").replace(">", "")
                    .replace(this.getClass().getName()+":", "").strip();
            String[] arr = name.split(" ");
            if (arr.length > 1) name = arr[arr.length-1];

            String value = entry.getValue();
            if (value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length()-1);

            res.put(name, value);
        }
        return res;
    }
}