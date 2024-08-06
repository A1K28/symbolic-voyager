package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

public class Z3ListCollectionTest {
    @SymbolicTest({0,1})
    @DisplayName("List: test_remove_by_idx_1")
    public int test_remove_by_idx_1(String a) {
        List<String> list = new ArrayList<>();
        list.add(a+10);
        if (list.remove(0).equals("TEST PARAM10")) {
            return 0;
        }
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("List: test_remove_1")
    public int test_remove_1(String a) {
        List<String> list = new ArrayList<>();
        list.add(a+10);
        if (list.remove("ASD10")) {
            return 0;
        }
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("List: test_remove_2")
    public int test_remove_2(Integer a, Integer b) {
        List<Integer> list = new ArrayList<>();
        list.add(a+b+100);
        Integer k = 300;
        if (list.remove(k)) {
            return 0;
        }
        return 1;
    }

    @SymbolicTest({0,1})
    @DisplayName("List: test_remove_all_1")
    public int test_remove_all_1(int a) {
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

    @SymbolicTest({0,2})
    @DisplayName("List: test_remove_all_2")
    public int test_remove_all_2(int a) {
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

    @SymbolicTest({0,1,2})
    @DisplayName("List: test_remove_all_3")
    public int test_remove_all_3(int a, int b) {
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
}