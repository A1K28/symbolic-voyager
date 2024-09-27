package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class PrimitiveReturnsMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        String returnType = getReturnType(methodNode);
        if (opcode == IRETURN
                || opcode == LRETURN
                || opcode == FRETURN
                || opcode == DRETURN) {
            it.remove();
            int k = 0;
            switch (returnType) {
                case "Z":
                case "B":
                case "S":
                case "I":
                    k = 1;
                    it.add(new InsnNode(ICONST_0));
                    break;
                case "J":
                    k = 1;
                    it.add(new InsnNode(LCONST_0));
                    break;
                case "F":
                    k = 1;
                    it.add(new InsnNode(FCONST_0));
                    break;
                case "D":
                    k = 1;
                    it.add(new InsnNode(DCONST_0));
                    break;
            }
            it.add(new InsnNode(opcode));
            return k;
        }
        return 0;
    }

    private String getReturnType(MethodNode methodNode) {
        int returnTypeStartIndex = methodNode.desc.lastIndexOf(')') + 1;
        return methodNode.desc.substring(returnTypeStartIndex);
    }
}
