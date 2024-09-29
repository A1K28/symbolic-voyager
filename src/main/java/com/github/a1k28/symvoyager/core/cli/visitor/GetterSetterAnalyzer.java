package com.github.a1k28.symvoyager.core.cli.visitor;

import lombok.RequiredArgsConstructor;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

@RequiredArgsConstructor
public class GetterSetterAnalyzer {
    private int lines;
    private final Method method;

    public static boolean isGetterOrSetter(Method method) {
        try {
            if ((method.getName().startsWith("get") && "void".equals(method.getReturnType().toString()))
                    || (method.getName().startsWith("set") && "void".equals(method.getReturnType().toString()))) {
                GetterSetterAnalyzer analyzer = new GetterSetterAnalyzer(method);
                analyzer.analyze(method);
                return analyzer.lines < 5;
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void analyze(Method method) throws IOException {
        String className = method.getDeclaringClass().getName();
        String resourceName = className.replace(".", File.separator) + ".class";
        try (InputStream inputStream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Class file not found: " + resourceName);
            }
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (name.equals(method.getName())) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            public void visitInsn(int opcode) {
                                lines++;
                                super.visitInsn(opcode);
                            }

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                lines+=10;
                            }
                        };
                    }
                    return null;
                }
            }, ClassReader.SKIP_DEBUG);
        }
    }
}