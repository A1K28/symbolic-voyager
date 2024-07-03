package com.github.a1k28.evoc.core.mutation.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ListIterator;

public interface Mutator {
    int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it);
}
