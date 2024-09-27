package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public class IncrementMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == Opcodes.IINC) {
            IincInsnNode iincInsn = (IincInsnNode) node;
            iincInsn.incr *= -1;
        }
        return 0;
    }
}
