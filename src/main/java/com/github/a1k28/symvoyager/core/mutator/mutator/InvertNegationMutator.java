package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public class InvertNegationMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == Opcodes.INEG) {
            it.add(new InsnNode(Opcodes.INEG));
            return 1;
        }
        return 0;
    }
}
