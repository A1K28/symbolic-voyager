package com.github.a1k28.evoc.helper;

import lombok.NoArgsConstructor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
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

    public static byte[] convertClassNodeToBytes(ClassNode classNode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    public static Class<?> loadClass(final String className, final byte[] classBytes) throws ClassNotFoundException {
        return new ClassLoader(ASMHelper.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    return defineClass(name, classBytes, 0, classBytes.length);
                }
                return super.loadClass(name);
            }
        }.loadClass(className);
    }

    public static Class<?> convertClassNodeToClass(ClassNode classNode) {
        try {
            String className = classNode.name.replace('/', '.');
            byte[] classBytes = convertClassNodeToBytes(classNode);
            return loadClass(className, classBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert ClassNode to Class", e);
        }
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
