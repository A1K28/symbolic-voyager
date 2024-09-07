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
    public int test_fields_2(int param2) {
        if (param2 > 10)
            field2 = 100;
        if (field2 == 100)
            return 0;
        if (param2 <= 0)
            field2 = 200;
        if (field2 == 200)
            return 1;
        return 2;
    }

    private int test_fields_1_get_updated_int(int param2) {
        return param2 + 100;
    }
}