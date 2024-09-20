package com.github.a1k28.evoc.core.cli;

import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class LocalTypeExtractor {
    private final Set<String> variableTypes = new HashSet<>();

    public static Set<String> extract(Class clazz) {
        try {
            LocalTypeExtractor collector = new LocalTypeExtractor();
            collector.collectTypes(clazz);
            return collector.variableTypes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void collectTypes(Class<?> clazz) throws IOException {
        String className = clazz.getName();
        String resourceName = className.replace(".", File.separator) + ".class";
        try (InputStream inputStream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Class file not found: " + resourceName);
            }
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    String type = Type.getType(descriptor).getClassName();
                    variableTypes.add(type);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    Type[] argumentTypes = Type.getArgumentTypes(descriptor);
                    for (int i = 0; i < argumentTypes.length; i++) {
                        String paramType = argumentTypes[i].getClassName();
                        variableTypes.add(paramType);
                    }

                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                            if (!name.equals("this")) {
                                String type = Type.getType(descriptor).getClassName();
                                variableTypes.add(type);
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG);
        }
    }
}