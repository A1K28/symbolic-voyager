package com.github.a1k28.evoc.core.asm;

// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html
public class ASMConditionalWrapper {
    public static boolean IF_ICMPEQ(int a, int b) {
        return a != b;
    }

    public static boolean IF_ICMPNE(int a, int b) {
        return a == b;
    }

    public static boolean IF_ICMPLT(int a, int b) {
        return a >= b;
    }

    public static boolean IF_ICMPLE(int a, int b) {
        return a > b;
    }

    public static boolean IF_ICMPGT(int a, int b) {
        return a <= b;
    }

    public static boolean IF_ICMPGE(int a, int b) {
        return a < b;
    }

    public static boolean IFEQ_LONG(long a, long b) {
        return a != b;
    }

    public static boolean IFNE_LONG(long a, long b) {
        return a == b;
    }

    public static boolean IFLT_LONG(long a, long b) {
        return a >= b;
    }

    public static boolean IFLE_LONG(long a, long b) {
        return a > b;
    }

    public static boolean IFGT_LONG(long a, long b) {
        return a <= b;
    }

    public static boolean IFGE_LONG(long a, long b) {
        return a < b;
    }

    public static boolean IFEQ_FLOAT(float a, float b) {
        return a != b;
    }

    public static boolean IFNE_FLOAT(float a, float b) {
        return a == b;
    }

    public static boolean IFLT_FLOAT(float a, float b) {
        return a >= b;
    }

    public static boolean IFLE_FLOAT(float a, float b) {
        return a > b;
    }

    public static boolean IFGT_FLOAT(float a, float b) {
        return a <= b;
    }

    public static boolean IFGE_FLOAT(float a, float b) {
        return a < b;
    }

    public static boolean IFEQ_DOUBLE(double a, double b) {
        return a != b;
    }

    public static boolean IFNE_DOUBLE(double a, double b) {
        return a == b;
    }

    public static boolean IFLT_DOUBLE(double a, double b) {
        return a >= b;
    }

    public static boolean IFLE_DOUBLE(double a, double b) {
        return a > b;
    }

    public static boolean IFGT_DOUBLE(double a, double b) {
        return a <= b;
    }

    public static boolean IFGE_DOUBLE(double a, double b) {
        return a < b;
    }

    public static boolean IFEQ_STRING(String a, String b) {
        return a.equals(b);
    }

    public static boolean IFNE_STRING(String a, String b) {
        return a.equals(b);
    }
}
