package com.github.a1k28.evoc.core.mutator.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class VoidMethodMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == INVOKEVIRTUAL
                || opcode == INVOKESPECIAL
                || opcode == INVOKESTATIC
                || opcode == INVOKEINTERFACE
                || opcode == INVOKEDYNAMIC) {
            if (node instanceof MethodInsnNode n) {
                if (n.desc.endsWith(")V")) {
                    it.remove();
                    return -1;
                }
            }
        }
        return 0;
    }
}
