package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

public class Z3MiscTest {
    @SymbolicTest({0,1})
    @DisplayName("test_ternary_operator_1")
    public int test_ternary_operator_1(int param) {
        int k = param > 10 ? 20 : -1;
        if (k == -1)
            return 0;
        return 1;
    }

//    @SymbolicTest({0,1})
//    @DisplayName("test_enhanced_for_loop_1")
//    public int test_enhanced_for_loop_1(int param) {
//    }

    @SymbolicTest({0,1,2,3})
    @DisplayName("test_switch_statements_1")
    public int test_switch_statements_1(int param) {
        String a;
        int k;
        switch (param) {
            case 0:
                a = "ASD";
                k = 0;
                break;
            case 1:
                k = 1;
                break;
            case 2:
                k = 2;
                break;
            default:
                k = 3;
        }
        return k;
    }

    @SymbolicTest({1,2,3})
    @DisplayName("test_switch_statements_2")
    public int test_switch_statements_2(int param) {
        int k;
        switch (param) {
            case 0:
                k = 0;
            case 1:
                k = 1;
                break;
            case 2:
                k = 2;
                break;
            default:
                k = 3;
        }
        return k;
    }

    @SymbolicTest({1,2,3})
    @DisplayName("test_switch_statements_3")
    public int test_switch_statements_3(String param) {
        int k;
        switch (param) {
            case "KEY1":
                k = 0;
            case "KEY2":
                k = 1;
                break;
            case "KEY3":
                k = 2;
                break;
            default:
                k = 3;
        }
        return k;
    }

    @SymbolicTest({1,2})
    @DisplayName("test_for_loop_1")
    public int test_for_loop_1(int param) {
        int i = 0;
        for (i = 0; i < param; i++) {
            if (i == 5)
                break;
        }
        if (i > 10)
            return 0;
        if (i == 3)
            return 1;
        return 2;
    }

    @SymbolicTest({1,2,3})
    @DisplayName("test_for_loop_2")
    public int test_for_loop_2(int param) {
        int k = 0;
        for (int i = 0; i < param; i++) {
            if (i == 5) continue;
            k = i;
        }
        if (k == 5)
            return 0;
        if (k == 3)
            return 1;
        if (k > 10)
            return 2;
        return 3;
    }
}