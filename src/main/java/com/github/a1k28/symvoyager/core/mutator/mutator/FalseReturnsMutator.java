package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class FalseReturnsMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == IRETURN || opcode == ARETURN) {
            String returnType = getReturnType(methodNode);
            if (returnType.equals("Z")) {
                it.remove();
                it.add(new FieldInsnNode(GETSTATIC, getClassNameRaw(Boolean.class), "FALSE", getClassName(Boolean.class)));
                it.add(new InsnNode(opcode));
                return 1;
            } else if (returnType.equals(getClassName(Boolean.class))) {
                it.remove();
                it.previous();
                it.remove();
                it.add(new FieldInsnNode(GETSTATIC, getClassNameRaw(Boolean.class), "FALSE", getClassName(Boolean.class)));
                it.add(new InsnNode(opcode));
                return 0;
            }
        }
        return 0;
    }

    private String getReturnType(MethodNode methodNode) {
        int returnTypeStartIndex = methodNode.desc.lastIndexOf(')') + 1;
        return methodNode.desc.substring(returnTypeStartIndex);
    }

    private String getClassName(Class clazz) {
        return "L" + getClassNameRaw(clazz) + ";";
    }

    private String getClassNameRaw(Class clazz) {
        return clazz.getName().replace(".", File.separator);
    }
}
