package com.github.a1k28.evoc.core.mutator.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class ConditionalsBoundaryMutator implements Mutator {
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
        if (opcode == IFGE) return IFGT;
        if (opcode == IFGT) return IFGE;
        if (opcode == IFLE) return IFLT;
        if (opcode == IFLT) return IFLE;

        if (opcode == IF_ICMPGE) return IF_ICMPGT;
        if (opcode == IF_ICMPGT) return IF_ICMPGE;
        if (opcode == IF_ICMPLE) return IF_ICMPLT;
        if (opcode == IF_ICMPLT) return IF_ICMPLE;

        return -1;
    }
}
