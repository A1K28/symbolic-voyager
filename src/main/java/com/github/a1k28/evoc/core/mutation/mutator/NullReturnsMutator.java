package com.github.a1k28.evoc.core.mutation.mutator;

import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class NullReturnsMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        if (opcode == ARETURN) {
            it.remove();
            it.add(new InsnNode(ACONST_NULL));
            it.add(new InsnNode(ARETURN));
            return 1;
        }
        return 0;
    }
}
