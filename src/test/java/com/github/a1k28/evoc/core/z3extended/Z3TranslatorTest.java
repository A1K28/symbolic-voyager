package com.github.a1k28.evoc.core.z3extended;

import com.microsoft.z3.Context;
import com.microsoft.z3.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Z3TranslatorTest {
    @Test
    public void testZ3() {
//        System.setProperty("java.library.path", "/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin");
//        System.loadLibrary("z3");
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");

        try (Context ctx = new Context()) {
            assertNotNull(Version.getString());
        }
    }
}