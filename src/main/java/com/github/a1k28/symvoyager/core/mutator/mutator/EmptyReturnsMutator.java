package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.tree.*;

import java.io.File;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class EmptyReturnsMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == ARETURN) {
            String returnType = getReturnType(methodNode);
            if (returnType.equals(getClassName(String.class))) {
                it.remove();
                it.add(new LdcInsnNode(""));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Optional.class))) {
                it.remove();
                String optionalTypeRaw = getClassNameRaw(Optional.class);
                String optionalType = getClassName(Optional.class);
                it.add(new MethodInsnNode(GETSTATIC, optionalTypeRaw, "empty()", "()" + optionalType, false));
                it.add(new InsnNode(ARETURN));
                return 1;
            }
            if (returnType.equals(getClassName(List.class))) {
                it.remove();
                String collectionsType = getClassNameRaw(Collections.class);
                String listType = getClassName(List.class);
                it.add(new MethodInsnNode(GETSTATIC, collectionsType, "emptyList()", "()" + listType, false));
                it.add(new InsnNode(ARETURN));
                return 1;
            }
            if (returnType.equals(getClassName(Collection.class))) {
                it.remove();
                String collectionsType = getClassNameRaw(Collections.class);
                String listType = getClassName(List.class);
                it.add(new MethodInsnNode(GETSTATIC, collectionsType, "emptyList()", "()" + listType, false));
                it.add(new InsnNode(ARETURN));
                return 1;
            }
            if (returnType.equals(getClassName(Set.class))) {
                it.remove();
                String collectionsType = getClassNameRaw(Collections.class);
                String setType = getClassName(Set.class);
                it.add(new MethodInsnNode(GETSTATIC, collectionsType, "emptySet()", "()" + setType, false));
                it.add(new InsnNode(ARETURN));
                return 1;
            }
            if (returnType.equals(getClassName(Integer.class))) {
                it.remove();
                it.add(new InsnNode(ICONST_0));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Short.class))) {
                it.remove();
                it.add(new InsnNode(ICONST_0));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Long.class))) {
                it.remove();
                it.add(new InsnNode(LCONST_0));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Character.class))) {
                it.remove();
                it.add(new InsnNode(ICONST_0));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Float.class))) {
                it.remove();
                it.add(new InsnNode(FCONST_0));
                it.add(new InsnNode(opcode));
                return 1;
            }
            if (returnType.equals(getClassName(Double.class))) {
                it.remove();
                it.add(new InsnNode(DCONST_0));
                it.add(new InsnNode(opcode));
                return 1;
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
