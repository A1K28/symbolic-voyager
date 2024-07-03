package com.github.a1k28.evoc.helper;

import lombok.NoArgsConstructor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;

@NoArgsConstructor
public class ASMHelper {
    public final static List<Integer> genericComparisonOpCodes = List.of(
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
    );
    public final static List<Integer> intComparisonOpCodes = List.of(
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE
    );
    public final static List<Integer> refComparisonOpCodes = List.of(
            IF_ACMPEQ, IF_ACMPNE
    );

    public static void saveClass(String classname, byte[] bytes) throws ClassNotFoundException, IOException {
        saveClass(classname, bytes, null);
    }

    public static void saveClass(String classname, byte[] bytes, String filename) throws ClassNotFoundException, IOException {
        String targetPath = getTargetPath(classname, filename);
        try (FileOutputStream fos = new FileOutputStream(targetPath)) {
            fos.write(bytes);
        }
    }

    public static byte[] getClassBytes(ClassNode cn) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static String getTargetPath(String classname) throws ClassNotFoundException {
        return getTargetPath(classname, null);
    }

    public static String getTargetPath(String classname, String filename) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(classname);
        String[] spl = classname.split("\\.");
        String simpleClassname = filename == null ? spl[spl.length-1] : filename;
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath()+clazz.getPackageName().replace(".", File.separator)+File.separator+simpleClassname+".class";
    }
}
