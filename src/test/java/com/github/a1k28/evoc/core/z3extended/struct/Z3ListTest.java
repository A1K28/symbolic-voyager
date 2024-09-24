package com.github.a1k28.evoc.core.z3extended.struct;

import com.android.tools.r8.internal.Ar;
import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

public class Z3ListTest {
    @SymbolicTest({4})
    @DisplayName("simple_index_test_1")
    public int simple_index_test_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1, 0, 2, 3
        if (!list.get(0).equals("1"))
            return 0;
        if (!list.get(1).equals("0"))
            return 1;
        if (!list.get(2).equals("2"))
            return 2;
        if (!list.get(3).equals("3"))
            return 3;
        return 4;
    }

//    @SymbolicTest({0,1,2,3,4})
    @DisplayName("simple_index_test_winput_1")
    public int simple_index_test_winput_1(List list) {
        if (list.get(0).equals("1"))
            return 0;
        if (list.get(1).equals("0"))
            return 1;
        if (list.get(2).equals("2"))
            return 2;
        if (list.get(3).equals("3"))
            return 3;
        return 4;
    }

    @SymbolicTest({9})
    @DisplayName("index_test_1")
    public int index_test_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1, 0, 2, 3
        if (!list.get(0).equals("1"))
            return 0;
        if (!list.get(1).equals("0"))
            return 1;
        if (!list.get(2).equals("2"))
            return 2;
        if (!list.get(3).equals("3"))
            return 3;

        list.add(3, "0"); // 1, 0, 2, 0, 3
        if (!list.get(0).equals("1"))
            return 4;
        if (!list.get(1).equals("0"))
            return 5;
        if (!list.get(2).equals("2"))
            return 6;
        if (!list.get(3).equals("0"))
            return 7;
        if (!list.get(4).equals("3"))
            return 8;
        return 9;
    }

    @SymbolicTest({7})
    @DisplayName("index_test_with_remove_1")
    public int index_test_with_remove_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1,0,2,3
        list.remove(2); // 1,0,3
        if (!list.get(0).equals("1"))
            return 0;
        if (!list.get(1).equals("0"))
            return 1;
        if (!list.get(2).equals("3"))
            return 2;

        list.add(0, "-1"); // -1,1,0,3
        list.add(3, "-2");  // -1,1,0,-2,3
        list.remove(1); // -1,0,-2,3
        if (!list.get(0).equals("-1")) // 0 -> 4
            return 3;
        if (!list.get(1).equals("0")) // 1 -> 3
            return 4;
        if (!list.get(2).equals("-2")) // 2 -> 5
            return 5;
        if (!list.get(3).equals("3")) // 3 -> 2
            return 6;
        return 7;
    }

    @SymbolicTest({2})
    @DisplayName("index_test_with_size_1")
    public int index_test_with_size_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1,0,2,3
        list.remove(2); // 1,0,3
        if (list.size() != 3)
            return 0;

        list.add(0, "-1"); // -1,1,0,3
        list.add(3, "-2");  // -1,1,0,-2,3
        list.remove(1); // -1,0,-2,3
        if (list.size() != 4)
            return 1;
        return 2;
    }

    @SymbolicTest({1})
    @DisplayName("test_init_with_capacity_1")
    public int test_init_with_capacity_1() {
        List<String> list = new ArrayList<>(2);
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add(1, "0"); // 1,0,2,3,4
        if (list.size() != 5)
            return 0;
        return 1;
    }

    @SymbolicTest({2})
    @DisplayName("test_init_with_collection_1")
    public int test_init_with_collection_1() {
        List<Integer> list1 = new ArrayList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);

        List list2 = new ArrayList(list1);
        if (list1.size() != list2.size())
            return 0;
        if (!list1.equals(list2))
            return 1;
        return 2;
    }

    @SymbolicTest({3})
    @DisplayName("test_hashcode_1")
    public int test_hashcode_1() {
        List<Integer> list1 = new ArrayList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);

        List<Integer> list2 = new ArrayList<>();
        list2.add(1);
        list2.add(2);
        list2.add(3);

        int hashCode1 = list1.hashCode();
        int hashCode2 = list2.hashCode();

        if (hashCode1 != hashCode2)
            return 0;

        list1.remove(1);
        if (list1.hashCode() == list2.hashCode())
            return 1;

        list1.remove(0);
        list2.remove(0);
        list1.add(0, 2);
        if (list1.hashCode() != list2.hashCode())
            return 2;
        return 3;
    }

//    @SymbolicTest({14})
    @DisplayName("test_list_of_1")
    public int test_list_of_1() {
        List<Integer> list = List.of();
        if (!list.isEmpty())
            return 0;

        list = List.of(1);
        if (list.size() != 1)
            return 1;

        list = List.of(1,2);
        if (list.size() != 2)
            return 2;

        list = List.of(1,2,3);
        if (list.size() != 3)
            return 3;
        if (list.get(2) != 3)
            return 4;
        if (list.get(0) != 1)
            return 5;

        list = List.of(1,2,3,4);
        if (list.size() != 4)
            return 6;

        list = List.of(1,2,3,4,5);
        if (list.size() != 5)
            return 7;

        list = List.of(1,2,3,4,5,6);
        if (list.size() != 6)
            return 8;

        list = List.of(1,2,3,4,5,6,7);
        if (list.size() != 7)
            return 9;

        list = List.of(1,2,3,4,5,6,7,8);
        if (list.size() != 8)
            return 10;

        list = List.of(1,2,3,4,5,6,7,8,9);
        if (list.size() != 9)
            return 11;

        list = List.of(1,2,3,4,5,6,7,8,9,10);
        if (list.size() != 10)
            return 12;

//        list = List.of(1,2,3,4,5,6,7,8,9,10,11);
//        if (list.size() != 11)
//            return 13;
        return 14;
    }

    @SymbolicTest({15})
    @DisplayName("test_remove_by_value_1")
    public int test_remove_by_value_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("1");
        list.add("2");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1,0,1,2,2,3
        list.remove("3"); // 1,0,1,2,2
        if (!list.get(0).equals("1"))
            return 0;
        if (!list.get(1).equals("0"))
            return 1;
        if (!list.get(2).equals("1"))
            return 2;
        if (!list.get(3).equals("2"))
            return 3;
        if (!list.get(4).equals("2"))
            return 4;

        list.add("-1"); // 1,0,1,2,2,-1
        list.remove("1"); // 0,1,2,2,-1

        if (!list.get(0).equals("0"))
            return 5;
        if (!list.get(1).equals("1"))
            return 6;
        if (!list.get(2).equals("2"))
            return 7;
        if (!list.get(3).equals("2"))
            return 8;
        if (!list.get(4).equals("-1"))
            return 9;

        list.remove("2"); // 0,1,2,-1
        list.remove(0); // 1,2,-1
        list.add("-2"); // 1,2,-1,-2

        if (!list.get(0).equals("1"))
            return 10;
        if (!list.get(1).equals("2"))
            return 11;
        if (!list.get(2).equals("-1"))
            return 12;
        if (!list.get(3).equals("-2"))
            return 13;
        if (list.remove("-3"))
            return 14;

        return 15;
    }

//    @SymbolicTest({3})
    @DisplayName("test_equals_1")
    public int test_equals_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");
        list1.add(1, "0"); // 1,0,2,3
        list1.remove(2); // 1,0,3

        List<String> list2 = new ArrayList<>();
        list2.add("1");
        list2.add("0");
        list2.add("2");
        list2.add("3");
        list2.add(1, "0"); // 1,0,0,2,3

        if (list1.equals(list2))
            return 0;

        list2.remove(2); // 1,0,2,3
        if (list2.equals(list1))
            return 1;

        list2.remove(2); // 1,0,3
        if (!list2.equals(list1))
            return 2;

        return 3;
    }

    @SymbolicTest({4})
    @DisplayName("test_set_1")
    public int test_set_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");

        if (!list.set(0, "-1").equals("1"))
            return 0;
        if (!list.set(2, "-1").equals("3"))
            return 1;
        if (!list.set(0, "-10").equals("-1"))
            return 2;
        if (list.size() != 3)
            return 3;
        return 4;
    }

    @SymbolicTest({7})
    @DisplayName("test_add_all_1")
    public int test_add_all_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<>();
        list2.add("4");
        list2.add("2");

        if (!list1.addAll(list2))
            return 0;
        if (!list1.get(0).equals("1"))
            return 1;
        if (!list1.get(1).equals("2"))
            return 2;
        if (!list1.get(2).equals("3"))
            return 3;
        if (!list1.get(3).equals("4"))
            return 4;
        if (!list1.get(4).equals("2"))
            return 5;
        if (list1.size() != 5)
            return 6;
        return 7;
    }

    @SymbolicTest({7})
    @DisplayName("test_add_all_by_index_1")
    public int test_add_all_by_index_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<>();
        list2.add("4");
        list2.add("2");

        if (!list1.addAll(2, list2)) // 1,2,4,2,3
            return 0;
        if (!list1.get(0).equals("1"))
            return 1;
        if (!list1.get(1).equals("2"))
            return 2;
        if (!list1.get(2).equals("4"))
            return 3;
        if (!list1.get(3).equals("2"))
            return 4;
        if (!list1.get(4).equals("3"))
            return 5;
        if (list1.size() != 5)
            return 6;
        return 7;
    }

    @SymbolicTest({5})
    @DisplayName("test_remove_all_1")
    public int test_remove_all_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<>();
        list2.add("2");
        list2.add("4");

        if (!list1.removeAll(list2))
            return 0;
        if (list1.size() != 2)
            return 1;
        if (!list1.contains("1"))
            return 2;
        if (!list1.contains("3"))
            return 3;
        if (list1.contains("2"))
            return 4;
        return 5;
    }

    @SymbolicTest({5})
    @DisplayName("test_retain_all_1")
    public int test_retain_all_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<>();
        list2.add("2");
        list2.add("4");

        if (!list1.retainAll(list2))
            return 0;
        if (list1.size() != 1)
            return 1;
        if (!list1.contains("2"))
            return 2;
        if (list1.contains("1"))
            return 3;
        if (list1.contains("3"))
            return 4;
        return 5;
    }

    @SymbolicTest({8})
    @DisplayName("test_contains_value_1")
    public int test_contains_value_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add(1, "0"); // 1,0,2,3
        list.remove(2); // 1,0,3
        if (!list.contains("3"))
            return 0;
        if (list.contains("2"))
            return 1;
        list.add(0, "-1"); // -1,1,0,3
        list.add(3, "-2");  // -1,1,0,-2,3
        list.remove(1); // -1,0,-2,3
        if (list.contains("-3"))
            return 3;
        if (list.contains("1"))
            return 4;
        if (!list.contains("0"))
            return 5;
        if (!list.contains("-1"))
            return 6;
        if (!list.contains("-2"))
            return 7;
        return 8;
    }

    @SymbolicTest({4})
    @DisplayName("test_contains_all_1")
    public int test_contains_all_1() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<>();
        list2.add("1");
        list2.add("2");

        if (list2.containsAll(list1))
            return 0;

        list1.remove(2);
        if (!list2.containsAll(list1))
            return 1;

        list1.add(0, "-1");
        list2.add("-2");
        if (list2.containsAll(list1))
            return 2;

        list2.add("-1");
        if (!list2.containsAll(list1))
            return 3;
        return 4;
    }

    @SymbolicTest({3})
    @DisplayName("test_clear_1")
    public int test_clear_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");

        if (list.isEmpty())
            return 0;
        list.clear();
        if (!list.isEmpty())
            return 1;
        list.add("200");
        if (list.size() != 1)
            return 2;
        return 3;
    }

//    //@SymbolicTest({3})
    @DisplayName("test_sublist_1")
    public int test_sublist_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");

        List<String> subList = list.subList(1,4);
        if (!subList.contains("2"))
            return 0;
        if (!subList.contains("3"))
            return 1;
        if (!subList.contains("4"))
            return 2;
        return 3;
    }

    @SymbolicTest({6})
    @DisplayName("test_index_of_1")
    public int test_index_of_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");

        if (list.indexOf("1") != 0)
            return 0;
        if (list.indexOf("2") != 1)
            return 1;
        if (list.indexOf("3") != 2)
            return 2;
        if (list.indexOf("4") != 3)
            return 3;
        if (list.indexOf("5") != 4)
            return 4;
        if (list.indexOf("2000") != -1)
            return 5;
        return 6;
    }

    @SymbolicTest({5})
    @DisplayName("test_index_of_2")
    public int test_index_of_2() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("2");
        list.add("2");
        list.add("5");

        if (list.indexOf("1") != 0)
            return 0;
        if (list.indexOf("2") != 1)
            return 1;
        if (list.indexOf("5") != 4)
            return 2;
        if (list.indexOf("2") != 1)
            return 3;
        if (list.indexOf("2000") != -1)
            return 4;
        return 5;
    }

    @SymbolicTest({5})
    @DisplayName("test_index_of_3")
    public int test_index_of_3() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("2");
        list.add("2");
        list.add("5");

        list.add(5, "-1");
        list.add(2, "-2");

        // current state: 1,2,-2,2,2,5,-1

        if (list.indexOf("1") != 0)
            return 0;
        if (list.indexOf("2") != 1)
            return 1;
        if (list.indexOf("-2") != 2)
            return 2;
        if (list.indexOf("5") != 5)
            return 3;
        if (list.indexOf("-1") != 6)
            return 4;
        return 5;
    }

    @SymbolicTest({4})
    @DisplayName("test_index_of_4")
    public int test_index_of_4() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("2");
        list.add("2");
        list.add("5");

        list.add(5, "-1");
        list.remove(2);

        // current state: 1,2,2,5,-1

        list.add(2, "-2");
        list.remove(5);


        // current state: 1,2,-2,2,5

        if (list.indexOf("1") != 0)
            return 0;
        if (list.indexOf("2") != 1)
            return 1;
        if (list.indexOf("-2") != 2)
            return 2;
        if (list.indexOf("5") != 4)
            return 3;
        return 4;
    }

    @SymbolicTest({3})
    @DisplayName("test_last_index_of_1")
    public int test_last_index_of_1() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("2");
        list.add("2");
        list.add("5");

        list.remove(2);

        if (list.lastIndexOf("1") != 0)
            return 0;
        if (list.lastIndexOf("2") != 2)
            return 1;
        if (list.lastIndexOf("5") != 3)
            return 2;
        if (list.lastIndexOf("2") == 2)
            return 3;
        return 4;
    }

    @SymbolicTest({4})
    @DisplayName("test_multi_dimensional_list_1")
    public int test_multi_dimensional_list_1() {
        List outer = new ArrayList();
        List inner1 = new ArrayList();
        List inner2 = new ArrayList();
        List innerInner1 = new ArrayList();

        inner1.add("1");
        inner2.add("2");
        innerInner1.add("3");

        outer.add(inner1);
        outer.add(inner2);
        inner1.add(innerInner1);

        List retrieved1 = (List) outer.get(0);
        if (!retrieved1.get(0).equals("1"))
            return 0;

        List retrieved2 = (List) ((List) outer.get(0)).get(1);
        if (retrieved2.size() != 1)
            return 1;

        if (!retrieved2.get(0).equals("3"))
            return 2;

        if (retrieved1.get(0).equals("2"))
            return 3;

        return 4;
    }









//    //@SymbolicTest({0,1})
    @DisplayName("test_remove_by_idx_1")
    public int test_remove_by_idx_1(String a) {
        List<String> list = new ArrayList<>();
        list.add(a+10);
        if (list.remove(0).equals("TEST PARAM10")) {
            return 0;
        }
        return 1;
    }

    ////@SymbolicTest({0,1})
    @DisplayName("test_remove_1")
    public int test_remove_1(String a) {
        List<String> list = new ArrayList<>();
        list.add(a+10);
        if (list.remove("ASD10")) {
            return 0;
        }
        return 1;
    }

    ////@SymbolicTest({0,1})
    @DisplayName("test_remove_2")
    public int test_remove_2(Integer a, Integer b) {
        List<Integer> list = new ArrayList<>();
        list.add(a+b+100);
        Integer k = 300;
        if (list.remove(k)) {
            return 0;
        }
        return 1;
    }

    ////@SymbolicTest({0,1})
    @DisplayName("test_remove_all_1")
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

    ////@SymbolicTest({0,2})
    @DisplayName("test_remove_all_2")
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

    ////@SymbolicTest({0,1,2})
    @DisplayName("test_remove_all_3")
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