package com.github.a1k28.evoc.core.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class ASMExample {
    private static final String internalWrapperName = "com/github/a1k28/evoc/core/asm/ASMConditionalWrapper";

    private final static List<Integer> genericComparisonOpCodes = List.of(
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
    );
    private final static List<Integer> intComparisonOpCodes = List.of(
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE
    );
    private final static List<Integer> refComparisonOpCodes = List.of(
            IF_ACMPEQ, IF_ACMPNE
    );

    private final Map<String, byte[]> classCache = new HashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException, AnalyzerException {
//        ClassReader classReader = new ClassReader("com.github.a1k28.test.Stack");
//        ClassWriter classWriter = new ClassWriter(classReader, 0);
//        ClassVisitor classVisitor = new ASMConditionalReplacer(classWriter);
//
//        classReader.accept(classVisitor, 0);

        ASMExample asmExample = new ASMExample();
        String classname = "com.github.a1k28.test.Stack";
        try {
            asmExample.snapClass(classname);
            asmExample.injectConditionalWrapper(classname);
            // run tests
        } finally {
            asmExample.restoreClass(classname);
        }
    }

    private void snapClass(String classname) throws IOException {
        ClassReader classReader = new ClassReader(classname);
        ClassNode cn = new ClassNode(ASM9);
        classReader.accept(cn, 0);
        byte[] bytes = getClassBytes(cn);
        classCache.put(classname, bytes);
    }

    private void restoreClass(String classname) throws IOException, ClassNotFoundException {
        byte[] bytes = classCache.get(classname);
        saveClass(classname, bytes);
    }

    private void injectConditionalWrapper(String classname) throws ClassNotFoundException, IOException, AnalyzerException {
        ClassReader classReader = new ClassReader(classname);
        ClassNode cn = new ClassNode(ASM9);
        classReader.accept(cn, 0);

        Iterator<MethodNode> methods = cn.methods.iterator();
        while (methods.hasNext()) {
            MethodNode mn = methods.next();
            if (mn.name.equals("main") || mn.name.equals("<init>")) continue;

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            Frame<BasicValue>[] frames = analyzer.analyze(cn.name, mn);

            ListIterator<AbstractInsnNode> it = mn.instructions.iterator();

            int offset = 0;
            while(it.hasNext()) {
                AbstractInsnNode node = it.next();
                int index = mn.instructions.indexOf(node);
                Frame<BasicValue> frame = frames[index-offset];
                if (frame == null) continue;

                int opcode = node.getOpcode();

                System.out.println(opcode);

                // string comparison
                if (isEqualsComparison(node)) {
                    handleStringComparison(node, it);
                    continue;
                }

                // int comparison
                if (intComparisonOpCodes.contains(opcode)) {
                    offset++;
                    handleIntComparisonCodes(node, it);
                    continue;
                }

                // long comparison
                if (opcode == LCMP) {
                    handleComparisonCodes(node, it, PrimType.LONG);
                    continue;
                }

                // float comparison
                if (opcode == FCMPG || opcode == FCMPL) {
                    handleComparisonCodes(node, it, PrimType.FLOAT);
                    continue;
                }

                // float comparison
                if (opcode == DCMPL || opcode == DCMPG) {
                    handleComparisonCodes(node, it, PrimType.DOUBLE);
                    continue;
                }
            }
        }

        byte[] modifiedClass = getClassBytes(cn);
        saveClass(classname, modifiedClass);
    }

    private void saveClass(String classname, byte[] bytes) throws ClassNotFoundException, IOException {
        String targetPath = getTargetPath(classname);
        try (FileOutputStream fos = new FileOutputStream(targetPath)) {
            fos.write(bytes);
        }
    }

    private byte[] getClassBytes(ClassNode cn) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static String getTargetPath(String classname) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(classname);
        String[] spl = classname.split("\\.");
        String simpleClassname = spl[spl.length-1];
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath()+clazz.getPackageName().replace(".",File.separator)+File.separator+simpleClassname+".class";
    }

    private static boolean isEqualsComparison(AbstractInsnNode node) {
        if (node.getOpcode() == INVOKEVIRTUAL) {
            MethodInsnNode n = (MethodInsnNode) node;
            if (n.owner.equals("java/lang/String")
                    && n.name.equals("equals")
                    && n.desc.equals("(Ljava/lang/Object;)Z")) {
                return true;
            }
        }
        return false;
    }

    private static void handleStringComparison(
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it) {
        int nextOpcode = node.getNext().getOpcode();
        if (IFEQ != nextOpcode && IFNE != nextOpcode) return;

        it.remove();
        String name = nextOpcode == IFEQ ? "IFEQ_STRING" : "IFNE_STRING";
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, "(Ljava/lang/String;Ljava/lang/String;)Z", false));
    }

    private static void handleIntComparisonCodes(
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it) {
        it.remove();
        String name = getIntNameByCode(node.getOpcode());
        LabelNode label = ((JumpInsnNode) node).label;
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, "(II)Z", false));
        it.add(new JumpInsnNode(IFEQ, label));
    }

    private static void handleComparisonCodes(
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it, PrimType type) {
        AbstractInsnNode next = node.getNext();
        int nextOpCode = next.getOpcode();
        if (!genericComparisonOpCodes.contains(nextOpCode)) return;

        it.remove();
        it.next();
        it.remove();

        String name = getGenericNameByCode(nextOpCode) + getMethodSuffix(type);
        String description = getDescriptor(type);
        LabelNode label = ((JumpInsnNode) next).label;
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, description, false));
        it.add(new JumpInsnNode(IFEQ, label));
    }

    private static String getMethodSuffix(PrimType type) {
        if (type == PrimType.LONG) return "_LONG";
        if (type == PrimType.FLOAT) return "_FLOAT";
        if (type == PrimType.DOUBLE) return "_DOUBLE";
        throw new RuntimeException("Invalid type: " + type);
    }

    private static String getDescriptor(PrimType type) {
        if (type == PrimType.LONG) return "(JJ)Z";
        if (type == PrimType.FLOAT) return "(FF)Z";
        if (type == PrimType.DOUBLE) return "(DD)Z";
        throw new RuntimeException("Invalid type: " + type);
    }

    private static BasicValue[] getTop2Types(Frame<BasicValue> frame) {
        assert frame.getStackSize() >= 2;
        BasicValue topValue1 = frame.getStack(frame.getStackSize() - 1);
        BasicValue topValue2 = frame.getStack(frame.getStackSize() - 2);
//        return new String[]{getTypeName(topValue1), getTypeName(topValue2)};
        return new BasicValue[]{topValue1, topValue2};
    }

    private static String getGenericNameByCode(int code) {
        if (code == IFEQ) return "IFEQ";
        if (code == IFNE) return "IFNE";
        if (code == IFLT) return "IFLT";
        if (code == IFGE) return "IFGE";
        if (code == IFGT) return "IFGT";
        if (code == IFLE) return "IFLE";
        throw new RuntimeException("Invalid code: " + code);
    }

    private static String getIntNameByCode(int code) {
        if (code == IF_ICMPEQ) return "IF_ICMPEQ";
        if (code == IF_ICMPNE) return "IF_ICMPNE";
        if (code == IF_ICMPLT) return "IF_ICMPLT";
        if (code == IF_ICMPGE) return "IF_ICMPGE";
        if (code == IF_ICMPGT) return "IF_ICMPGT";
        if (code == IF_ICMPLE) return "IF_ICMPLE";
        return null;
    }

    private static String getTypeName(BasicValue value) {
        if (value == BasicValue.INT_VALUE) {
            return "int";
        } else if (value == BasicValue.FLOAT_VALUE) {
            return "float";
        } else if (value == BasicValue.LONG_VALUE) {
            return "long";
        } else if (value == BasicValue.DOUBLE_VALUE) {
            return "double";
        } else if (value == BasicValue.REFERENCE_VALUE) {
            return "object";
        } else {
            return "unknown";
        }
    }
}