package com.github.a1k28.symvoyager.core.mutator.mutator;

import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class NegateConditionalsMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        int opposite = getOpposite(opcode);
        if (opposite != -1) {
            it.remove();
            LabelNode label = ((JumpInsnNode) node).label;
            it.add(new JumpInsnNode(opposite, label));
        }
        return 0;
    }

    private int getOpposite(int opcode) {
        // generic
        if (opcode == IFEQ) return IFNE;
        if (opcode == IFNE) return IFEQ;
        if (opcode == IFLT) return IFGE;
        if (opcode == IFGE) return IFLT;
        if (opcode == IFGT) return IFLE;
        if (opcode == IFLE) return IFGT;

        // ints
        if (opcode == IF_ICMPEQ) return IF_ICMPNE;
        if (opcode == IF_ICMPNE) return IF_ICMPEQ;
        if (opcode == IF_ICMPLT) return IF_ICMPGE;
        if (opcode == IF_ICMPGE) return IF_ICMPLT;
        if (opcode == IF_ICMPGT) return IF_ICMPLE;
        if (opcode == IF_ICMPLE) return IF_ICMPGT;

        // refs
        if (opcode == IF_ACMPEQ) return IF_ACMPNE;
        if (opcode == IF_ACMPNE) return IF_ACMPEQ;

        return -1;
    }
}
