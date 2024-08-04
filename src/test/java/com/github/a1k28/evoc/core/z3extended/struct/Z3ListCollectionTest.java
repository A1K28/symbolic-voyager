package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.SymbolicPathCarver;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.github.a1k28.evoc.core.z3extended.Z3Translator.close;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Z3ListCollectionTest {
    private SymbolicPathCarver symbolicPathCarver;
    private int asd;
    private final String classname = "com.github.a1k28.evoc.core.z3extended.struct.Z3ListCollectionTest";

    @BeforeEach
    public void setup() {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        symbolicPathCarver = new SymbolicPathCarver(
//                "com.github.a1k28.evoc.core.executor.SymbolicPathCarverTest");
    }

    @AfterAll
    public static void closeContext() {
        close();
    }

    @Test
    public void test_remove_all() throws ClassNotFoundException {
        SatisfiableResults sr = new SymbolicPathCarver(
                classname, "test_remove_all_1").analyzeSymbolicPaths();
        Set<Integer> reachableCodes = new HashSet<>(List.of(0, 1));
        for (SatisfiableResult res : sr.getResults()) {
            int expected = Integer.parseInt(res.getReturnValue().getName());
            int actual = test_remove_all_1(Integer.parseInt(res.getParameter("a")));
            assertEquals(expected, actual);
            assertTrue(reachableCodes.remove(expected));
        }
        assertTrue(reachableCodes.isEmpty());

        sr = new SymbolicPathCarver(
                classname, "test_remove_all_2").analyzeSymbolicPaths();
        reachableCodes = new HashSet<>(List.of(0, 2));
        for (SatisfiableResult res : sr.getResults()) {
            int expected = Integer.parseInt(res.getReturnValue().getName());
            int actual = test_remove_all_2(Integer.parseInt(res.getParameter("a")));
            assertEquals(expected, actual);
            assertTrue(reachableCodes.remove(expected));
        }
        assertTrue(reachableCodes.isEmpty());

        sr = new SymbolicPathCarver(
                classname, "test_remove_all_3").analyzeSymbolicPaths();
        reachableCodes = new HashSet<>(List.of(0, 1, 2));
        for (SatisfiableResult res : sr.getResults()) {
            int expected = Integer.parseInt(res.getReturnValue().getName());
            int actual = test_remove_all_3(Integer.parseInt(res.getParameter("a")), Integer.parseInt(res.getParameter("b")));
            assertEquals(expected, actual);
            assertTrue(reachableCodes.remove(expected));
        }
        assertTrue(reachableCodes.isEmpty());
    }

    private int test_remove_all_1(int a) {
        List<Integer> list = new ArrayList<>();
        list.add(a+10);
        list.add(1, 100);
        if (list.removeAll(List.of(100))) {
            if (list.isEmpty()) {
                System.out.println("ADWAD");
                return 0;
            }
            return 1;
        }
        return 2;
    }

    private int test_remove_all_2(int a) {
        List<Integer> list = new ArrayList<>();
        list.add(a+10);
        if (list.removeAll(List.of(100))) {
            if (list.isEmpty()) {
                System.out.println("ADWAD");
                return 0;
            }
            return 1;
        }
        return 2;
    }

    private int test_remove_all_3(int a, int b) {
        List<Integer> list = new ArrayList<>();
        list.add(a+10);
        list.add(b);
        if (list.removeAll(List.of(100))) {
            if (list.isEmpty()) {
                System.out.println("ADWAD");
                return 0;
            }
            return 1;
        }
        return 2;
    }

    public void test_list_methods(int a) {
//        List<String> asdf = List.of("ASD", "f");
//        List.of("ASD");
//        asdf = List.of("213");

//        List<String> list3 = new ArrayList<>(List.of("ASD"));
//        new ArrayList<>(10);
//        list3 = new ArrayList<>(10);

//        List<String> list3 = new ArrayList<>(List.of("ASD", "123"));
//        List<String> list2 = new ArrayList<>(10);

        List<Integer> list = new ArrayList<>();
//        List<Integer> intList = new ArrayList<>();
//
//        intList.addAll(List.of(1,2,3,4,5,6,7,8,9,10,11));
//        intList.size();

//        int[] arr = new int[1];
//        int[][] arr2 = new int[10][];
//        int[][] arr3 = new int[2][2];
//        int[][][] arr4 = new int[3][3][];
//
//        arr[0] = 2;
//        arr2[1][1] = 3;
//        arr4[1][1][0] = 2;

//        intList.stream().filter(e -> e % 21 == 1);

//        intList.hashCode();
//        intList.replaceAll(n -> n * 2);
//        intList.sort(Integer::compareTo);
//        intList.sort((e1, e2) -> e1.compareTo(e2));
//        intList.subList(1, 2);
//        list.size();
//        list.isEmpty();
//        list.add(a+"123");
//        list.add("ASDF");
//        list.add(null);
        list.add(a+10);
        list.add(1, 100);
//        list.add(100);
//        if (list.get(0).equals("ASD_123")) {
//            System.out.println("wdaw1er12r12rf");
//        }
//        Integer i = 100;
        if (list.removeAll(List.of(100))) {
            if (list.isEmpty()) {
                System.out.println("ADWAD");
            }
        }
//        if (list.remove(i)) {
//            System.out.println("ASDWD");
//        }
//        list.get(0);
//        list.add(0, "123");
//        list.addAll(List.of("asd", "123"));
//        list.addAll(1, List.of("asd", "123"));
//        list.remove("123");
//        list.remove(1);
//        list.removeAll(List.of("asd", "123"));
//        list.contains("123");
//        list.containsAll(List.of("asd", "123"));
//        list.retainAll(List.of("asd", "123"));
//        list.clear();
//        list.equals(List.of("asd", "123"));
//        list.set(1, "asd");
//        list.indexOf("asd");
//        list.lastIndexOf("asd");
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