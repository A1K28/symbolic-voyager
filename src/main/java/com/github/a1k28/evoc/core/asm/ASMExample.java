package com.github.a1k28.evoc.core.asm;

import com.github.a1k28.dclagent.ClassReloaderAPI;
import com.github.a1k28.evoc.helper.DynamicTestRunner;
import com.github.a1k28.evoc.helper.Logger;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * <p>Oracle JVM instruction set: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html</p>
 */
public class ASMExample {
    private static final Logger log = Logger.getInstance(ASMExample.class);
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

    public static void main(String[] args) throws Exception {
//        ClassReader classReader = new ClassReader("com.github.a1k28.test.Stack");
//        ClassWriter classWriter = new ClassWriter(classReader, 0);
//        ClassVisitor classVisitor = new ASMConditionalReplacer(classWriter);
//
//        classReader.accept(classVisitor, 0);

        Class<?> reloaderClass = Class.forName("com.github.a1k28.dclagent.DynamicClassAgent");

        // Get the instance of DynamicClassReloader
        Method getInstanceMethod = reloaderClass.getMethod("getInstance");
        ClassReloaderAPI reloader = (ClassReloaderAPI) getInstanceMethod.invoke(null);

        // Use the reloader

        ASMExample asmExample = new ASMExample();
        String classname = "com.github.a1k28.test.Stack";
        String testClassname = "com.github.a1k28.test.StackTest";

        try {
            asmExample.snapClass(classname);
            asmExample.injectConditionalWrapper(classname);

            // reload classes
            reloader.addClassToReload(classname, getTargetPath(classname));
            reloader.reloadClasses();

//             run tests
            Class<?> testClass = Class.forName(testClassname);
            for (Method method : testClass.getDeclaredMethods()) {
                ASMConditionalWrapper.testId = getMethodSignature(testClass, method);
                TestExecutionSummary summary = DynamicTestRunner.runTestClass(testClass, method);
            }
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
        classCache.remove(classname);
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

                String bId = mn.name + ":" + index;

                // string comparison
                if (isEqualsComparison(node)) {
                    offset++;
                    handleStringComparison(node, it, bId);
                    continue;
                }

                // int comparison
                if (intComparisonOpCodes.contains(opcode)) {
                    offset+=2;
                    handleIntComparisonCodes(node, it, bId);
                    continue;
                }

                // long comparison
                if (opcode == LCMP) {
                    offset++;
                    handleComparisonCodes(node, it, bId, PrimType.LONG);
                    continue;
                }

                // float comparison
                if (opcode == FCMPG || opcode == FCMPL) {
                    offset++;
                    handleComparisonCodes(node, it, bId, PrimType.FLOAT);
                    continue;
                }

                // float comparison
                if (opcode == DCMPL || opcode == DCMPG) {
                    offset++;
                    handleComparisonCodes(node, it, bId, PrimType.DOUBLE);
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

    public static String getTargetPath(String classname) throws ClassNotFoundException {
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
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it, String bId) {
        int nextOpcode = node.getNext().getOpcode();
        if (IFEQ != nextOpcode && IFNE != nextOpcode) return;

        it.remove();
        it.add(new LdcInsnNode(bId));
        String name = nextOpcode == IFEQ ? "IFEQ_STRING" : "IFNE_STRING";
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", false));
    }

    private static void handleIntComparisonCodes(
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it, String bId) {
        it.remove();
        String name = getIntNameByCode(node.getOpcode());
        LabelNode label = ((JumpInsnNode) node).label;
        it.add(new LdcInsnNode(bId));
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, "(IILjava/lang/String;)Z", false));
        it.add(new JumpInsnNode(IFEQ, label));
    }

    private static void handleComparisonCodes(
            AbstractInsnNode node, ListIterator<AbstractInsnNode> it, String bId, PrimType type) {
        AbstractInsnNode next = node.getNext();
        int nextOpCode = next.getOpcode();
        if (!genericComparisonOpCodes.contains(nextOpCode)) return;

        it.remove();
        it.next();
        it.remove();

        String name = getGenericNameByCode(nextOpCode) + getMethodSuffix(type);
        String description = getDescriptor(type);
        LabelNode label = ((JumpInsnNode) next).label;
        it.add(new LdcInsnNode(bId));
        it.add(new MethodInsnNode(INVOKESTATIC, internalWrapperName, name, description, false));
        it.add(new JumpInsnNode(IFEQ, label));
    }

    private static void handleGenericComparison(
            Frame<BasicValue> frame, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, String bId) {
        BasicValue[] types = getTop2Types(frame);
        if (types == null) {
            log.warn("Could not get top 2 types. It is likely that there is a bug with frame indexing.");
            return;
        }
        System.out.println("types: " + types[0] + " " + types[1]);
//        if (types[0] == BasicValue.INT_VALUE && types[1] == BasicValue.INT_VALUE)
//            handleComparisonCodes(node, it, bId, PrimType.INT);
//        if (types[0] == BasicValue.LONG_VALUE && types[1] == BasicValue.LONG_VALUE)
//            handleComparisonCodes(node, it, bId, PrimType.LONG);
//        if (types[0] == BasicValue.FLOAT_VALUE && types[1] == BasicValue.FLOAT_VALUE)
//            handleComparisonCodes(node, it, bId, PrimType.FLOAT);
//        if (types[0] == BasicValue.DOUBLE_VALUE && types[1] == BasicValue.DOUBLE_VALUE)
//            handleComparisonCodes(node, it, bId, PrimType.DOUBLE);
    }

    private static String getMethodSuffix(PrimType type) {
        if (type == PrimType.INT) return "_INT";
        if (type == PrimType.LONG) return "_LONG";
        if (type == PrimType.FLOAT) return "_FLOAT";
        if (type == PrimType.DOUBLE) return "_DOUBLE";
        throw new RuntimeException("Invalid type: " + type);
    }

    /*
     * B: byte
     * C: char
     * D: double
     * F: float
     * I: int
     * J: long
     * L: object (e.g., Ljava/lang/String;)
     * S: short
     * V: void
     * Z: boolean
     * [: array (e.g., [I for an int array)
     */
    private static String getDescriptor(PrimType type) {
        if (type == PrimType.INT) return "(IILjava/lang/String;)Z";
        if (type == PrimType.LONG) return "(JJLjava/lang/String;)Z";
        if (type == PrimType.FLOAT) return "(FFLjava/lang/String;)Z";
        if (type == PrimType.DOUBLE) return "(DDLjava/lang/String;)Z";
        throw new RuntimeException("Invalid type: " + type);
    }

    private static BasicValue[] getTop2Types(Frame<BasicValue> frame) {
        if (frame.getStackSize() < 2) return null;
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

    private static String getMethodSignature(Class<?> clazz, Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(clazz.getCanonicalName()).append(".")
                .append(method.getName())
                .append("(");
        for (int i = 0; i < method.getParameterCount(); i++) {
            Class<?> type = method.getParameterTypes()[i];
            sb.append(type.getName());
            if (i < method.getParameterCount()-1) sb.append(", ");
        }
        for (Class<?> type : method.getParameterTypes()) {
            sb.append(type.getName()).append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}