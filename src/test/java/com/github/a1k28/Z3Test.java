package com.github.a1k28;

import com.microsoft.z3.Context;
import com.microsoft.z3.Version;

public class Z3Test {
    public static void main(String[] args) {
//        System.setProperty("java.library.path", "/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin");
//        System.loadLibrary("z3");
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");

        Context ctx = new Context();
        System.out.println("Z3 version: " + Version.getString());
        ctx.close();
    }
}