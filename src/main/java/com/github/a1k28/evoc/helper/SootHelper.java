package com.github.a1k28.evoc.helper;

import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import sootup.core.IdentifierFactory;
import sootup.core.Project;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.*;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;
import sootup.java.core.language.JavaLanguage;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SootHelper {
    private static final Map<String, Class<?>> cachedMap = new HashMap<>();
    private static ArrayMap arrayMap = null;

    private static class ArrayMap {
        private ClassType classType;
        private MethodSignature initMethod;
        private MethodSignature addByIndexMethod;
        private MethodSignature getByIndexMethod;
    }

    public static SootClass<JavaSootClassSource> getSootClass(String className) throws ClassNotFoundException {
        int javaVersion = getJavaVersion(Class.forName(className));
        JavaLanguage language = new JavaLanguage(javaVersion);

        Optional<SootClass<JavaSootClassSource>> optional = getJavaSootClassSource(
                className, language);
        for (int i = 0; i < 3 && optional.isEmpty(); i++) {
            // the class may have not been loaded properly. wait for a bit.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            optional = getJavaSootClassSource(className, language);
        }

        return optional.get();
    }

    private static Optional<SootClass<JavaSootClassSource>> getJavaSootClassSource(
            String className, JavaLanguage language) {
        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        Project project = JavaProject.builder(language)
                .addInputLocation(inputLocation).build();

        ClassType classType =
                project.getIdentifierFactory().getClassType(className);

        View<?> view = project.createView();
        initArrayMap(view);

        return (Optional<SootClass<JavaSootClassSource>>) view.getClass(classType);
    }

    public static SootMethod getSootMethod(
            SootClass<JavaSootClassSource> sootClass, Executable method, boolean isConstructor) {
        // Get the method
        outer: for (SootMethod sootMethod : sootClass.getMethods()) {
            if (!isConstructor && !method.getName().equals(sootMethod.getName())) continue;
            List<Type> sootTypes = sootMethod.getParameterTypes();
            Class<?>[] types = method.getParameterTypes();
            if (types.length != sootTypes.size()) continue;
            for (int i = 0; i < types.length; i++)
                if (!types[i].getName().equals(sootTypes.get(i).toString()))
                    continue outer;
            return sootMethod;
        }
        throw new IllegalStateException("Could not match method: " + method);
    }

    public static Executable getMethod(AbstractInvokeExpr invokeExpr) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(invokeExpr.getMethodSignature().getDeclClassType().toString());
        boolean isConstructorCall = isConstructorCall(invokeExpr.getMethodSignature());
        Executable[] executables = isConstructorCall ?
                clazz.getDeclaredConstructors() : clazz.getDeclaredMethods();
        List<Type> sootParamTypes = invokeExpr.getMethodSignature().getParameterTypes();
        String methodName = invokeExpr.getMethodSignature().getName();
        outer: for (Executable method : executables) {
            if (!isConstructorCall && !method.getName().equals(methodName)) continue;
            Class<?>[] methodParamTypes = method.getParameterTypes();
            if (methodParamTypes.length != sootParamTypes.size()) continue;
            for (int i = 0; i < methodParamTypes.length; i++)
                if (!methodParamTypes[i].getName().equals(sootParamTypes.get(i).toString()))
                    continue outer;
            return method;
        }
        throw new IllegalStateException("Could not match method: " + invokeExpr.getMethodSignature());
    }

    public static int getJavaVersion(Class<?> clazz) {
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(
                clazz.getName().replace(".", File.separator) + ".class");
             DataInputStream dis = new DataInputStream(is)) {

            dis.readInt(); // Skip magic number
            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            return mapVersionToJava(majorVersion, minorVersion);
        } catch (IOException e) {
            throw new RuntimeException("Error reading class file", e);
        }
    }

    public static Class<?> getClass(ClassType classType) {
        return getClass(classType.getFullyQualifiedName());
    }

    public static Class<?> getClass(String classname) {
        try {
            if (!cachedMap.containsKey(classname)) {
                Class clazz = Class.forName(classname);
                cachedMap.put(classname, clazz);
            }
            return cachedMap.get(classname);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFlowDiagram(SMethodPath sMethodPath, Body body) {
        StmtGraph<?> cfg = body.getStmtGraph();
//        Stmt start = cfg.getStartingStmt();
        BasicBlock<?> block = cfg.getStartingStmtBlock();
//        print(cfg, start, 0);
//        dfs(cfg, start, sMethodPath, sMethodPath.getRoot());
        interpretSoot(block, sMethodPath, sMethodPath.getRoot());
    }

    public static List<JavaSootField> getFields(SootClass<?> sootClass) throws ClassNotFoundException {
        List<JavaSootField> fields = new ArrayList<>();
        addFields(sootClass, fields);
        return fields;
    }

    public static Class translateType(Type type) {
        try {
            if (type.getClass() == PrimitiveType.BooleanType.class)
                return boolean.class;
            if (type.getClass() == PrimitiveType.ByteType.class)
                return byte.class;
            if (type.getClass() == PrimitiveType.ShortType.class)
                return short.class;
            if (type.getClass() == PrimitiveType.CharType.class)
                return char.class;
            if (type.getClass() == PrimitiveType.IntType.class)
                return int.class;
            if (type.getClass() == PrimitiveType.LongType.class)
                return long.class;
            if (type.getClass() == PrimitiveType.FloatType.class)
                return float.class;
            if (type.getClass() == PrimitiveType.DoubleType.class)
                return double.class;
            if (type.getClass() == ArrayType.class)
                return translateType(((ArrayType) type).getElementType()).arrayType();
            if (type.getClass() == UnknownType.class)
                return Object.class;
            return Class.forName(type.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isConstructorCall(MethodSignature methodSignature) {
        return methodSignature.toString().matches(".*<.*: void <init>\\(.*\\)>.*");
    }

    private static void addFields(SootClass<?> sootClass, List<JavaSootField> fields) throws ClassNotFoundException {
        sootClass.getFields().forEach(e -> {if (e instanceof JavaSootField j) fields.add(j);});
        if (sootClass.hasSuperclass()) {
            // TODO: stop within the same package?
            String name = sootClass.getSuperclass().get().toString();
            if (!name.equals(Object.class.getName())) {
                SootClass<?> parent = SootHelper.getSootClass(sootClass.getSuperclass().get().toString());
                addFields(parent, fields);
            }
        }
    }

    public static String translateField(Field field) {
        return new StringBuilder("<")
                .append(field.getDeclaringClass().getName())
                .append(": ")
                .append(field.getType().getName())
                .append(" ")
                .append(field.getName())
                .append(">")
                .toString();
    }

    private static void print(StmtGraph<?> cfg, Stmt current, int level) {
        for (int i = 1; i < level; i++) System.out.print("\t");
        System.out.println(current);
        List<Stmt> succs = cfg.getAllSuccessors(current);
        succs.forEach(e -> print(cfg, e, level+1));
    }

//    private static void interpretSoot(BasicBlock<?> block,
//                                      SMethodPath sMethodPath,
//                                      SBlock parentBlock,
//                                      Map<BasicBlock<?>, List<SNode>> visited) {
//        for (Stmt current : block.getStmts()) {
//            SNode node = sMethodPath.getNode(current);
//            parentBlock.add(node);
//        }
//
//        SNode nextParent = sMethodPath.getSNodeMap().get(block.getTail()).get(0);
//        if (nextParent.getType() == SType.GOTO)
//            return;
//
//        if (nextParent.getType() == SType.BRANCH) {
//            parentBlock.removeLastStmt();
//
//            List<BasicBlock<?>> successors = (List<BasicBlock<?>>) block.getSuccessors();
//            assert successors.size() == 2;
//
//            Optional<SNode> ifOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_FALSE);
//            Optional<SNode> elseOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_TRUE);
//
//            if (ifOpt.isPresent()) {
//                parentBlock.addSucc(sMethodPath.getBlock(ifOpt.get()).get());
//            } else {
//                SBlock ifBlock = new SBlock(BlockType.BRANCH_BLOCK);
//                SNode ifNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_FALSE);
//                ifBlock.add(ifNode);
//                parentBlock.addSucc(ifBlock);
//                interpretSoot(successors.get(0), sMethodPath, ifBlock, visited);
//            }
//
//            if (elseOpt.isPresent()) {
//                parentBlock.addSucc(sMethodPath.getBlock(elseOpt.get()).get());
//            } else {
//                SBlock elseBlock = new SBlock(BlockType.BRANCH_BLOCK);
//                SNode elseNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_TRUE);
//                elseBlock.add(elseNode);
//                parentBlock.addSucc(elseBlock);
//                interpretSoot(successors.get(1), sMethodPath, elseBlock, visited);
//            }
//        } else {
//            for (BasicBlock<?> successor : block.getSuccessors()) {
//                SBlock childBlock = new SBlock();
//                parentBlock.addSucc(childBlock);
//                interpretSoot(successor, sMethodPath, childBlock, visited);
//            }
//        }
//
//        // handle catch blocks
////        Map<ClassType, BasicBlock> catchBlocks
////                = (Map<ClassType, BasicBlock>) block.getExceptionalSuccessors();
////        for (Map.Entry<ClassType, BasicBlock> entry : catchBlocks.entrySet()) {
////            ClassType exceptionType = entry.getKey();
////            BasicBlock<?> catchBlock = entry.getValue();
////            Optional<SBlock> optional = sMethodPath.getRootBlock().getExceptionBlock(catchBlock.getHead());
////            if (optional.isPresent()) {
//////                parentBlock.addSucc(optional.get());
////                continue;
////            }
////            SBlock catchChild = new SBlock(exceptionType);
//////            parentBlock.addSucc(catchChild);
////            interpretSoot(catchBlock, sMethodPath, catchChild, visited);
////        }
//    }

    private static void interpretSoot(BasicBlock<?> block,
                                      SMethodPath sMethodPath,
                                      SNode parent) {
        List<Stmt> stmts = block.getStmts();
        int i = 0;
        if (parent.getType() == SType.CATCH) i = 1;

        for (;i < stmts.size(); i++) {
            Stmt current = stmts.get(i);
            if (parent.containsParent(current)) continue;

            if (current instanceof JAssignStmt<?,?> assignStmt) {
                SNode result = handleArrays(assignStmt, sMethodPath, parent);
                if (result != null) {
                    parent = result;
                    continue;
                }
            }

            SNode node = sMethodPath.getNode(current);
            parent.addChild(node);
            parent = node;
        }

        if (parent.getType() == SType.GOTO)
            return;

        if (parent.getType() == SType.SWITCH) {
            parent = handleSwitch(parent, sMethodPath, block);
        } else if (parent.getType() == SType.BRANCH) {
            parent = handleBranch(parent, sMethodPath, block);
        } else {
            for (BasicBlock<?> successor : block.getSuccessors()) {
                interpretSoot(successor, sMethodPath, parent);
            }
        }

        // handle catch blocks
//        Map<ClassType, BasicBlock> catchBlocks
//                = (Map<ClassType, BasicBlock>) block.getExceptionalSuccessors();
//        for (Map.Entry<ClassType, BasicBlock> entry : catchBlocks.entrySet()) {
//            ClassType classType = entry.getKey();
//            BasicBlock<?> catchBlock = entry.getValue();
//            Optional<SNode> optional = sMethodPath.getNodeOptional(catchBlock.getHead(), SType.CATCH);
//            if (optional.isPresent()) {
//                if (parent.containsParent(optional.get().getUnit())) continue;
//                parent.addChild(optional.get());
//                continue;
//            }
//            SCatchNode node = (SCatchNode) sMethodPath.createNode(catchBlock.getHead());
//            if (parent.containsParent(node.getUnit())) continue;
//            node.setExceptionType(classType);
//            parent.addChild(node);
//            interpretSoot(catchBlock, sMethodPath, node);
//        }
    }

    private static SNode handleArrays(JAssignStmt assignStmt, SMethodPath sMethodPath, SNode parent) {
        if (assignStmt.getRightOp() instanceof JNewArrayExpr jNewArrayExpr) {
            ClassType classType = arrayMap.classType;
            JNewExpr jNewExpr = Jimple.newNewExpr(classType);
            JAssignStmt newAssignStmt = Jimple.newAssignStmt(
                    assignStmt.getLeftOp(), jNewExpr, assignStmt.getPositionInfo());

            SNode assignmentNode = sMethodPath.getNode(newAssignStmt);
            parent.addChild(assignmentNode);

            Immediate capacity = jNewArrayExpr.getSize();
            MethodSignature constructor = arrayMap.initMethod;
            JSpecialInvokeExpr specialInvokeExpr = Jimple
                    .newSpecialInvokeExpr((Local) assignStmt.getLeftOp(), constructor, capacity);
            JInvokeStmt invokeStmt = Jimple.newInvokeStmt(
                    specialInvokeExpr, assignStmt.getPositionInfo());

            SNode invokeNode = sMethodPath.getNode(invokeStmt);
            parent.addChild(invokeNode);

            return invokeNode;
        } else if (assignStmt.getLeftOp() instanceof JArrayRef arrayRef) {
            Local base = arrayRef.getBase();
            Immediate index = arrayRef.getIndex();
            Immediate value = (Immediate) assignStmt.getRightOp();
            MethodSignature addMethodSignature = arrayMap.addByIndexMethod;
            JInterfaceInvokeExpr interfaceInvoke = Jimple
                    .newInterfaceInvokeExpr(base, addMethodSignature, index, value);
            JInvokeStmt invokeStmt = Jimple.newInvokeStmt(
                    interfaceInvoke, assignStmt.getPositionInfo());
            SNode invokeNode = sMethodPath.getNode(invokeStmt);
            parent.addChild(invokeNode);

            return invokeNode;
        } else if (assignStmt.getRightOp() instanceof JArrayRef arrayRef) {
            Local base = arrayRef.getBase();
            Immediate index = arrayRef.getIndex();
            Immediate leftOp = (Immediate) assignStmt.getLeftOp();
            MethodSignature addMethodSignature = arrayMap.getByIndexMethod;
            JInterfaceInvokeExpr interfaceInvoke = Jimple
                    .newInterfaceInvokeExpr(base, addMethodSignature, index);
            JAssignStmt jAssignStmt = Jimple.newAssignStmt(
                    leftOp, interfaceInvoke, assignStmt.getPositionInfo());

            SNode invokeNode = sMethodPath.getNode(jAssignStmt);
            parent.addChild(invokeNode);

            return invokeNode;
        }
        return null;
    }

    private static SNode handleSwitch(SNode parent, SMethodPath sMethodPath, BasicBlock<?> block) {
        JSwitchStmt switchStmt = (JSwitchStmt) parent.getUnit();
        List<IntConstant> values = switchStmt.getValues();
        parent = parent.getParent();
        parent.removeLastChild();
        for (int k = 0; k < values.size(); k++) {
            JEqExpr eqExpr = Jimple.newEqExpr(switchStmt.getKey(), values.get(k));
            JIfStmt ifStmt = Jimple.newIfStmt(eqExpr, switchStmt.getPositionInfo());
            SNode ifNode = sMethodPath.createNode(ifStmt);
            SNode elseNode = sMethodPath.createNode(ifStmt);
            parent.addChild(ifNode);
            parent.addChild(elseNode);
            ifNode.setType(SType.BRANCH_TRUE);
            elseNode.setType(SType.BRANCH_FALSE);
            interpretSoot(block.getSuccessors().get(k), sMethodPath, ifNode);
            parent = elseNode;
        }
        if (values.size() + 1 == block.getSuccessors().size())
            interpretSoot(block.getSuccessors().get(values.size()), sMethodPath, parent);
        return parent;
    }

    private static SNode handleBranch(SNode parent, SMethodPath sMethodPath, BasicBlock<?> block) {
        parent = parent.getParent();
        parent.removeLastChild();

        List<BasicBlock<?>> successors = (List<BasicBlock<?>>) block.getSuccessors();
        assert successors.size() == 2;

        Optional<SNode> ifOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_FALSE);
        Optional<SNode> elseOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_TRUE);

        if (ifOpt.isPresent()) {
            parent.addChild(ifOpt.get());
        } else {
            SNode ifNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_FALSE);
            parent.addChild(ifNode);
            interpretSoot(successors.get(0), sMethodPath, ifNode);
        }

        if (elseOpt.isPresent()) {
            parent.addChild(elseOpt.get());
        } else {
            SNode elseNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_TRUE);
            parent.addChild(elseNode);
            interpretSoot(successors.get(1), sMethodPath, elseNode);
        }
        return parent;
    }

    // interpret soot graph
    private static void dfs(
            StmtGraph<?> cfg,
            Stmt current,
            SMethodPath sMethodPath,
            SNode parent) {
        if (sMethodPath.getSNodeMap().containsKey(current)) {
            parent.addChild(sMethodPath.getSNodeMap().get(current).get(0));
            return;
        }
        SNode node = sMethodPath.createNode(current);
        parent.addChild(node);
        if (!cfg.getTails().contains(current)) {
            List<Stmt> succs = cfg.getAllSuccessors(current);
            if (node.getType() == SType.SWITCH) {
                parent.removeLastChild();
                JSwitchStmt switchStmt = (JSwitchStmt) current;
                List<IntConstant> values = switchStmt.getValues();
                for (int i = 0; i < values.size(); i++) {
                    JEqExpr eqExpr = Jimple.newEqExpr(switchStmt.getKey(), values.get(i));
                    JIfStmt ifStmt = Jimple.newIfStmt(eqExpr, current.getPositionInfo());
                    SNode ifNode = sMethodPath.createNode(ifStmt);
                    SNode elseNode = sMethodPath.createNode(ifStmt);
                    parent.addChild(ifNode);
                    parent.addChild(elseNode);
                    ifNode.setType(SType.BRANCH_TRUE);
                    elseNode.setType(SType.BRANCH_FALSE);
                    dfs(cfg, succs.get(i), sMethodPath, ifNode);
                    parent = elseNode;
                }
                // has default
                if (values.size() + 1 == succs.size())
                    dfs(cfg, succs.get(values.size()), sMethodPath, parent);
            } else {
                if (node.getType() == SType.BRANCH) {
                    // assuming first two values are if & else branches
                    SNode node2 = sMethodPath.createNode(current);
                    parent.addChild(node2);

                    node.setType(SType.BRANCH_FALSE);
                    node2.setType(SType.BRANCH_TRUE);

                    dfs(cfg, succs.get(0), sMethodPath, node);
                    dfs(cfg, succs.get(1), sMethodPath, node2);

                    // handle catch blocks
                    for (int i = 2;i<succs.size();i++) {
                        Stmt succ = succs.get(i);
                        if (current == succ) continue;
                        if (!node.containsParent(succ)) dfs(cfg, succ, sMethodPath, node);
                        if (!node2.containsParent(succ)) dfs(cfg, succ, sMethodPath, node2);
                    }
                } else {
                    for (int i = 0; i < succs.size(); i++) {
                        Stmt succ = succs.get(i);
                        if (current != succ && !node.containsParent(succ)) {
                            dfs(cfg, succ, sMethodPath, node);
                        }
                    }
                }
            }
        }
    }

    private static void initArrayMap(View<?> view) {
        if (arrayMap == null) {
            IdentifierFactory identifierFactory = view.getIdentifierFactory();
            ClassType listClass = identifierFactory
                    .getClassType(List.class.getCanonicalName());
            ClassType arrayClass = identifierFactory
                    .getClassType(ArrayList.class.getCanonicalName());

            Type intType = identifierFactory.getType("int");
            MethodSignature constructor = identifierFactory.getMethodSignature(
                    arrayClass,
                    "<init>",
                    VoidType.getInstance(),
                    List.of(intType));

            Type objectType = identifierFactory.getType(Object.class.getCanonicalName());
            MethodSignature addMethod = identifierFactory.getMethodSignature(
                    listClass,
                    "add",
                    VoidType.getInstance(),
                    List.of(intType, objectType));

            MethodSignature getMethod = identifierFactory.getMethodSignature(
                    listClass,
                    "get",
                    objectType,
                    List.of(intType));

            arrayMap = new ArrayMap();
            arrayMap.classType = arrayClass;
            arrayMap.initMethod = constructor;
            arrayMap.addByIndexMethod = addMethod;
            arrayMap.getByIndexMethod = getMethod;
        }
    }

    private static int mapVersionToJava(int major, int minor) {
        return switch (major) {
            case 52 -> 8;
            case 53 -> 9;
            case 54 -> 10;
            case 55 -> 11;
            case 56 -> 12;
            case 57 -> 13;
            case 58 -> 14;
            case 59 -> 15;
            case 60 -> 16;
            case 61 -> 17;
            case 62 -> 18;
            case 63 -> 19;
            case 64 -> 20;
            case 65 -> 21;
            default -> throw new RuntimeException("Unknown (major version: " + major + ", minor version: " + minor + ")");
        };
    }
}
