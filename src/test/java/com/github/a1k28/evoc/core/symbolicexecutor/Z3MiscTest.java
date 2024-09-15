package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.outsidescope.NOPService;
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

//    //@SymbolicTest({0,1})
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

    @SymbolicTest({1,2,3})
    @DisplayName("test_nested_for_loop_1")
    public int test_nested_for_loop_1(int param) {
        int k = 0;
        for (int i = 0; i < param; i++) {
            if (i == 5) continue;
            for (int j = 0; j < 3; j++) {
                k += 1;
            }
        }
        if (k == 5)
            return 0;
        if (k == 3)
            return 1;
        if (k > 10)
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_method_mock_1")
    public int test_method_mock_1() {
        NOPService nopService = new NOPService();
        int res = nopService.calculate();
        if (res == -1)
            return 0;
        if (res == 10)
            return 1;
        return 2;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_method_mock_2")
    public int test_method_mock_2(int a, int b) {
        NOPService nopService = new NOPService();
        int res = nopService.calculate(a, b);
        if (res == -1)
            return 0;
        if (res == 10)
            return 1;
        return 2;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_try_catch_1")
    public int test_try_catch_1(int param) {
        if (param == 10)
            return 0;
        try {
            if (param == 20)
                throw new IllegalArgumentException("Illegal arguments provided");
            return 1;
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

    @SymbolicTest({0,1,2,3})
    @DisplayName("test_try_catch_2")
    public int test_try_catch_2(int param) {
        try {
            if (param == 10)
                return 0;
            if (param == 20)
                throw new IllegalArgumentException("Illegal arguments provided");
            if (param == 30)
                throw new IllegalStateException("Illegal state exception");
            return 1;
        } catch (IllegalArgumentException e) {
            return 2;
        } catch (IllegalStateException e) {
            return 3;
        }
    }

    @SymbolicTest({1})
    @DisplayName("test_try_catch_finally_1")
    public int test_try_catch_finally_1(int param) {
        try {
            if (param == 10)
                return 0;
        } catch (IllegalArgumentException e) {
            return 2;
        } catch (IllegalStateException e) {
            return 3;
        } finally {
            System.out.println("ASD");
            return 1;
        }
    }

    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_nested_try_catch_1")
    public int test_nested_try_catch_1(int param) {
        try {
            try {
                if (param == 10)
                    return 0;
                if (param == 20 || param == 25)
                    throw new IllegalArgumentException("Illegal arguments provided");
                if (param == 30)
                    throw new IllegalStateException("Illegal state exception");
                return 1;
            } catch (IllegalArgumentException e) {
                if (param == 25)
                    throw new IllegalStateException("Illegal state exception 2");
                return 2;
            } catch (IllegalStateException e) {
                return 3;
            }
        } catch (IllegalStateException e) {
            return 4;
        }
    }

    @SymbolicTest({0,1,2,3,4,5,6})
    @DisplayName("test_nested_try_catch_2")
    public int test_nested_try_catch_2(int param) {
        try {
            try {
                try {
                    if (param == 10)
                        return 0;
                    if (param == 20 || param == 25 || param == 27 || param == 28)
                        throw new IllegalArgumentException("Illegal arguments provided");
                    if (param == 30)
                        throw new IllegalStateException("Illegal state exception");
                    return 1;
                } catch (IllegalArgumentException e) {
                    if (param == 25 || param == 27 || param == 28)
                        throw new IllegalStateException("Illegal state exception 2");
                    return 2;
                } catch (IllegalStateException e) {
                    return 3;
                }
            } catch (IllegalStateException e) {
                if (param == 25)
                    throw new ArrayIndexOutOfBoundsException();
                if (param == 27)
                    throw new IllegalArgumentException("Illegal arguments provided 2");
                return 4;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return 5;
        } catch (RuntimeException e) {
            return 6;
        }
    }

    @SymbolicTest({0})
    @DisplayName("test_nested_try_catch_finally_1")
    public int test_nested_try_catch_finally_1(int param) {
        try {
            try {
                try {
                    if (param == 10)
                        return 0;
                    if (param == 20 || param == 25 || param == 27 || param == 28)
                        throw new IllegalArgumentException("Illegal arguments provided");
                    if (param == 30)
                        throw new IllegalStateException("Illegal state exception");
                    return 1;
                } catch (IllegalArgumentException e) {
                    if (param == 25 || param == 27 || param == 28)
                        throw new IllegalStateException("Illegal state exception 2");
                    return 2;
                } catch (IllegalStateException e) {
                    return 3;
                }
            } catch (IllegalStateException e) {
                if (param == 25)
                    throw new ArrayIndexOutOfBoundsException();
                if (param == 27)
                    throw new IllegalArgumentException("Illegal arguments provided 2");
                return 4;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return 5;
        } catch (RuntimeException e) {
            return 6;
        } finally {
            return 0;
        }
    }
}