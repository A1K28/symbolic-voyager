package com.github.a1k28.evoc.core.mutator.mutator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class MathMutator implements Mutator {
    @Override
    public int mutate(int opcode, AbstractInsnNode node, ListIterator<AbstractInsnNode> it, MethodNode methodNode) {
        int opposite = getOpposite(opcode);
        if (opposite != -1) {
            it.remove();
            it.add(new InsnNode(opposite));
        }
        return 0;
    }

    private int getOpposite(int opcode) {
        // ints
        if (opcode == IADD) return ISUB;
        if (opcode == ISUB) return IADD;
        if (opcode == IMUL) return IDIV;
        if (opcode == IDIV) return IMUL;
        if (opcode == IREM) return IMUL;
        if (opcode == IAND) return IOR;
        if (opcode == IOR) return IAND;
        if (opcode == IXOR) return IAND;
        if (opcode == ISHL) return ISHR;
        if (opcode == ISHR) return ISHL;
        if (opcode == IUSHR) return ISHL;

        // longs
        if (opcode == LADD) return LSUB;
        if (opcode == LSUB) return LADD;
        if (opcode == LMUL) return LDIV;
        if (opcode == LDIV) return LMUL;
        if (opcode == LREM) return LMUL;
        if (opcode == LAND) return LOR;
        if (opcode == LOR) return LAND;
        if (opcode == LXOR) return LAND;
        if (opcode == LSHL) return LSHR;
        if (opcode == LSHR) return LSHL;
        if (opcode == LUSHR) return LSHL;

        // floats
        if (opcode == FADD) return FSUB;
        if (opcode == FSUB) return FADD;
        if (opcode == FMUL) return FDIV;
        if (opcode == FDIV) return FMUL;
        if (opcode == FREM) return FMUL;

        return -1;
    }
}
