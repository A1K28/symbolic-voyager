package com.github.a1k28.evoc.core.mutation;

import com.github.a1k28.evoc.core.mutation.mutator.*;
import com.github.a1k28.evoc.core.mutation.struct.MType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;

import static com.github.a1k28.evoc.helper.ASMHelper.getClassBytes;
import static com.github.a1k28.evoc.helper.ASMHelper.saveClass;
import static org.objectweb.asm.Opcodes.ASM9;

public class MutationFactory {
    public static String mutate(String className, String methodName, MType type)
            throws IOException, ClassNotFoundException, AnalyzerException {
        ClassReader classReader = new ClassReader(className);
        ClassNode cn = new ClassNode(ASM9);
        classReader.accept(cn, 0);

        Mutator mutator = getMutator(type);

        Iterator<MethodNode> methods = cn.methods.iterator();
        while (methods.hasNext()) {
            MethodNode mn = methods.next();
            if (!mn.name.equals(methodName)) continue;

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            Frame<BasicValue>[] frames = analyzer.analyze(cn.name, mn);

            ListIterator<AbstractInsnNode> it = mn.instructions.iterator();

            int offset = 0;
            while(it.hasNext()) {
                AbstractInsnNode node = it.next();
                int index = mn.instructions.indexOf(node);
                Frame<BasicValue> frame = frames[index-offset];
                if (frame == null) continue;

                int opcode = node.getOpcode();
                System.out.println(opcode);

                offset += mutator.mutate(opcode, node, it, mn);
            }
        }

        String[] split = className.split("\\.");
        String filename = getName(split[split.length-1], type);

        byte[] modifiedClass = getClassBytes(cn);
        saveClass(className, modifiedClass, filename);

        return filename;
    }

    public static String getName(String filename, MType type) {
        return filename + "_" + type;
    }

    private static Mutator getMutator(MType type) {
        if (type == MType.CONDITIONALS_BOUNDARY) return new ConditionalsBoundaryMutator();
        if (type == MType.INCREMENTS) return new IncrementMutator();
        if (type == MType.INVERT_NEGATIVES) return new InvertNegationMutator();
        if (type == MType.MATH) return new MathMutator();
        if (type == MType.NEGATE_CONDITIONALS) return new NegateConditionalsMutator();
        if (type == MType.VOID_METHOD_CALLS) return new VoidMethodMutator();
        if (type == MType.EMPTY_RETURNS) return new EmptyReturnsMutator();
        if (type == MType.FALSE_RETURNS) return new FalseReturnsMutator();
        if (type == MType.TRUE_RETURNS) return new TrueReturnsMutator();
        if (type == MType.NULL_RETURNS) return new NullReturnsMutator();
        if (type == MType.PRIMITIVE_RETURNS) return new PrimitiveReturnsMutator();
        throw new RuntimeException("Could not create mutator type: " + type);
    }

    public static void main(String[] args) throws ClassNotFoundException, AnalyzerException, IOException {
        MutationFactory.mutate(
                "com.github.a1k28.Stack",
                "test_nr",
                MType.PRIMITIVE_RETURNS);
    }
}
