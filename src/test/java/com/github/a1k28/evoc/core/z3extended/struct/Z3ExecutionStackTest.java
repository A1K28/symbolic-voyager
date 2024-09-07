package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

public class Z3ExecutionStackTest {
    private String field1;
    private int field2;

    @SymbolicTest({2,3})
    @DisplayName("test_fields_1")
    public int test_fields_1(int param) {
        System.out.println("FIELD1: " + field1);
        System.out.println("FIELD2: " + field2);

        int k = test_fields_1_get_updated_int(param);
        if (param == k)
            return 0;
        if (k != param + 100)
            return 1;
        if (k == field2)
            return 2;
        return 3;
    }

    @SymbolicTest({0,1,2})
    @DisplayName("test_fields_2")
    public int test_fields_2(int param) {
        if (param > 10)
            field2 = 100;
        if (field2 == 100)
            return 0;
        if (param <= 0)
            field2 = 200;
        if (field2 == 200)
            return 1;
        return 2;
    }

    @SymbolicTest({0,1})
    @DisplayName("test_recursion_1")
    public int test_recursion_1(int param) {
        test_recursion_1_helper(param);
        if (this.field2 == 5)
            return 0;
        return 1;
    }

    @SymbolicTest({0,2,3})
    @DisplayName("test_recursion_2")
    public int test_recursion_2(String param) {
        String res = test_recursion_2_helper(param);
        if ("123".equals(res))
            return 0;
        if (res.equals("asd000"))
            return 1;
        if (res.equals("asd0000000"))
            return 2;
        return 3;
    }

    private String test_recursion_2_helper(String param) {
        if (param.length() == 10)
            return param;
        if (param.length() == 20)
            return "123";
        return test_recursion_2_helper(param+"0");
    }


    private int test_recursion_1_helper(int param) {
        if (param > 5)
            return param;
        this.field2 = param;
        return test_recursion_1_helper(param+1);
    }

    private int test_fields_1_get_updated_int(int param2) {
        return param2 + 100;
    }
}