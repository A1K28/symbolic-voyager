//package com.github.a1k28.symvoyager.core.sootup;
//
//import com.github.a1k28.symvoyager.core.sootup.model.ArrayMap;
//import com.github.a1k28.symvoyager.core.sootup.model.ExceptionBlock;
//import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
//import com.github.a1k28.symvoyager.core.symbolicexecutor.model.StmtWithPosition;
//import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SCatchNode;
//import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
//import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
//import lombok.AccessLevel;
//import lombok.NoArgsConstructor;
//import sootup.core.IdentifierFactory;
//import sootup.core.graph.BasicBlock;
//import sootup.core.jimple.Jimple;
//import sootup.core.jimple.basic.Immediate;
//import sootup.core.jimple.basic.LValue;
//import sootup.core.jimple.basic.Local;
//import sootup.core.jimple.common.constant.IntConstant;
//import sootup.core.jimple.common.expr.*;
//import sootup.core.jimple.common.ref.JArrayRef;
//import sootup.core.jimple.common.stmt.JAssignStmt;
//import sootup.core.jimple.common.stmt.JIfStmt;
//import sootup.core.jimple.common.stmt.JInvokeStmt;
//import sootup.core.jimple.common.stmt.Stmt;
//import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
//import sootup.core.model.Position;
//import sootup.core.signatures.MethodSignature;
//import sootup.core.types.ClassType;
//import sootup.core.types.Type;
//import sootup.core.types.VoidType;
//import sootup.core.views.View;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@NoArgsConstructor(access = AccessLevel.PRIVATE)
//class SootParser {
//    private static ArrayMap arrayMap = null;
//
//    static void interpretSoot(BasicBlock<?> block, SMethodPath sMethodPath) {
//        Set<ExceptionBlock> exceptionBlocks = new HashSet<>();
//        interpretSootBody(block, sMethodPath, sMethodPath.getRoot(), exceptionBlocks);
//        for (ExceptionBlock exceptionBlock : exceptionBlocks) {
//            BasicBlock<?> basicBlock = exceptionBlock.getBlock();
//            SCatchNode catchNode = sMethodPath.createCatchNode(
//                    basicBlock.getHead(), exceptionBlock.getType());
//            interpretSootBody(basicBlock, sMethodPath, catchNode, null);
//        }
//    }
//
//    private static void interpretSootBody(BasicBlock<?> block,
//                                          SMethodPath sMethodPath,
//                                          SNode parent,
//                                          Set<ExceptionBlock> exceptionBlocks) {
//        addExceptionsToHandlerMap(block, sMethodPath);
//
//        List<Stmt> stmts = block.getStmts();
//        int i = 0;
//
//        if (parent.getType() == SType.CATCH) i = 1;
//
//        boolean allCovered = true;
//        for (;i < stmts.size(); i++) {
//            Stmt current = stmts.get(i);
//            if (parent.containsParent(current)) continue;
//            allCovered = false;
//
//            if (current instanceof JAssignStmt assignStmt) {
//                SNode result = handleArrays(assignStmt, sMethodPath, parent);
//                if (result != null) {
//                    parent = result;
//                    continue;
//                }
//            }
//
//            SNode node = sMethodPath.getNode(current);
//            parent.addChild(node);
//            parent = node;
//        }
//
//        // avoid infinite recursion
//        if (allCovered) return;
//
//        if (exceptionBlocks != null) {
//            addCatchBlocksAndTheirDescendants(block, exceptionBlocks);
//        }
//
//        if (parent.getType() == SType.SWITCH) {
//            handleSwitch(parent, sMethodPath, block, exceptionBlocks);
//        } else if (parent.getType() == SType.BRANCH) {
//            handleBranch(parent, sMethodPath, block, exceptionBlocks);
//        } else {
//            for (BasicBlock<?> successor : block.getSuccessors()) {
//                interpretSootBody(successor, sMethodPath, parent, exceptionBlocks);
//            }
//        }
//    }
//
//    private static void addExceptionsToHandlerMap(BasicBlock block, SMethodPath sMethodPath) {
//        Map<ClassType, BasicBlock> exceptionalSuccessors = (Map<ClassType, BasicBlock>) block.getExceptionalSuccessors();
//        if (!exceptionalSuccessors.isEmpty()) {
//            Stmt head = block.getHead();
//            if (!sMethodPath.getHandlerMap().containsKey(head)) {
//                List<StmtWithPosition> stmtWithPositions = new ArrayList<>();
//                for (Map.Entry<ClassType, BasicBlock> entry : exceptionalSuccessors.entrySet()) {
//                    Stmt handlerStmt = entry.getValue().getHead();
//                    Position position = entry.getValue().getTail().getPositionInfo().getStmtPosition();
//                    stmtWithPositions.add(new StmtWithPosition(handlerStmt, position));
//                }
//                Collections.sort(stmtWithPositions,
//                        Comparator.comparingInt(e -> e.getPosition().getFirstLine()));
//                List<Stmt> s = stmtWithPositions.stream()
//                        .sorted(Comparator.comparingInt(e -> e.getPosition().getFirstLine()))
//                        .map(StmtWithPosition::getHandlerStmt)
//                        .collect(Collectors.toList());
//                sMethodPath.getHandlerMap().put(head, s);
//            }
//        }
//    }
//
//    private static void addCatchBlocksAndTheirDescendants(
//            BasicBlock<?> block, Set<ExceptionBlock> exceptionBlocks) {
//        Map<ClassType, BasicBlock> catchBlocks
//                = (Map<ClassType, BasicBlock>) block.getExceptionalSuccessors();
//        for (Map.Entry<ClassType, BasicBlock> entry : catchBlocks.entrySet()) {
//            ExceptionBlock exceptionBlock = new ExceptionBlock(entry.getKey(), entry.getValue());
//            if (exceptionBlocks.contains(exceptionBlock)) continue;
//            exceptionBlocks.add(exceptionBlock);
//            addCatchBlocksAndTheirDescendants(entry.getValue(), exceptionBlocks);
//        }
//    }
//
//    private static SNode handleArrays(JAssignStmt assignStmt, SMethodPath sMethodPath, SNode parent) {
//        if (assignStmt.getRightOp() instanceof JNewArrayExpr jNewArrayExpr) {
//            ClassType classType = arrayMap.getClassType();
//            JNewExpr jNewExpr = Jimple.newNewExpr(classType);
//            JAssignStmt newAssignStmt = Jimple.newAssignStmt(
//                    assignStmt.getLeftOp(), jNewExpr, assignStmt.getPositionInfo());
//
//            SNode assignmentNode = sMethodPath.getNode(newAssignStmt);
//            parent.addChild(assignmentNode);
//
//            Immediate capacity = jNewArrayExpr.getSize();
//            MethodSignature constructor = arrayMap.getInitMethod();
//            JSpecialInvokeExpr specialInvokeExpr = Jimple
//                    .newSpecialInvokeExpr((Local) assignStmt.getLeftOp(), constructor, capacity);
//            JInvokeStmt invokeStmt = Jimple.newInvokeStmt(
//                    specialInvokeExpr, assignStmt.getPositionInfo());
//
//            SNode invokeNode = sMethodPath.getNode(invokeStmt);
//            parent.addChild(invokeNode);
//
//            return invokeNode;
//        } else if (assignStmt.getLeftOp() instanceof JArrayRef arrayRef) {
//            Local base = arrayRef.getBase();
//            Immediate index = arrayRef.getIndex();
//            Immediate value = (Immediate) assignStmt.getRightOp();
//            MethodSignature addMethodSignature = arrayMap.getAddByIndexMethod();
//            JInterfaceInvokeExpr interfaceInvoke = Jimple
//                    .newInterfaceInvokeExpr(base, addMethodSignature, List.of(index, value));
//            JInvokeStmt invokeStmt = Jimple.newInvokeStmt(
//                    interfaceInvoke, assignStmt.getPositionInfo());
//            SNode invokeNode = sMethodPath.getNode(invokeStmt);
//            parent.addChild(invokeNode);
//
//            return invokeNode;
//        } else if (assignStmt.getRightOp() instanceof JArrayRef arrayRef) {
//            Local base = arrayRef.getBase();
//            Immediate index = arrayRef.getIndex();
//            LValue leftOp = assignStmt.getLeftOp();
//            MethodSignature addMethodSignature = arrayMap.getGetByIndexMethod();
//            JInterfaceInvokeExpr interfaceInvoke = Jimple
//                    .newInterfaceInvokeExpr(base, addMethodSignature, index);
//            JAssignStmt jAssignStmt = Jimple.newAssignStmt(
//                    leftOp, interfaceInvoke, assignStmt.getPositionInfo());
//
//            SNode invokeNode = sMethodPath.getNode(jAssignStmt);
//            parent.addChild(invokeNode);
//
//            return invokeNode;
//        }
//        return null;
//    }
//
//    private static void handleSwitch(SNode parent,
//                                     SMethodPath sMethodPath,
//                                     BasicBlock<?> block,
//                                     Set<ExceptionBlock> exceptionBlocks) {
//        JSwitchStmt switchStmt = (JSwitchStmt) parent.getUnit();
//        List<IntConstant> values = switchStmt.getValues();
//        parent = parent.getParent();
//        parent.removeLastChild();
//        for (int k = 0; k < values.size(); k++) {
//            JEqExpr eqExpr = Jimple.newEqExpr(switchStmt.getKey(), values.get(k));
//            JIfStmt ifStmt = Jimple.newIfStmt(eqExpr, switchStmt.getPositionInfo());
//            SNode ifNode = sMethodPath.createNode(ifStmt);
//            SNode elseNode = sMethodPath.createNode(ifStmt);
//            parent.addChild(ifNode);
//            parent.addChild(elseNode);
//            ifNode.setType(SType.BRANCH_TRUE);
//            elseNode.setType(SType.BRANCH_FALSE);
//            interpretSootBody(block.getSuccessors().get(k), sMethodPath, ifNode, exceptionBlocks);
//            parent = elseNode;
//        }
//        if (values.size() + 1 == block.getSuccessors().size())
//            interpretSootBody(block.getSuccessors().get(values.size()), sMethodPath, parent, exceptionBlocks);
//    }
//
//    private static void handleBranch(SNode parent,
//                                     SMethodPath sMethodPath,
//                                     BasicBlock<?> block,
//                                     Set<ExceptionBlock> exceptionBlocks) {
//        parent = parent.getParent();
//        parent.removeLastChild();
//
//        List<BasicBlock<?>> successors = (List<BasicBlock<?>>) block.getSuccessors();
//        assert successors.size() == 2;
//
//        Optional<SNode> ifOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_FALSE);
//        Optional<SNode> elseOpt = sMethodPath.getNodeOptional(block.getTail(), SType.BRANCH_TRUE);
//
//        if (ifOpt.isPresent()) {
//            parent.addChild(ifOpt.get());
//        } else {
//            SNode ifNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_FALSE);
//            parent.addChild(ifNode);
//            interpretSootBody(successors.get(0), sMethodPath, ifNode, exceptionBlocks);
//        }
//
//        if (elseOpt.isPresent()) {
//            parent.addChild(elseOpt.get());
//        } else {
//            SNode elseNode = sMethodPath.getNode(block.getTail(), SType.BRANCH_TRUE);
//            parent.addChild(elseNode);
//            interpretSootBody(successors.get(1), sMethodPath, elseNode, exceptionBlocks);
//        }
//    }
//
//    static void initArrayMap(IdentifierFactory identifierFactory) {
//        if (arrayMap == null) {
//            ClassType listClass = identifierFactory
//                    .getClassType(List.class.getCanonicalName());
//            ClassType arrayClass = identifierFactory
//                    .getClassType(ArrayList.class.getCanonicalName());
//
//            Type intType = identifierFactory.getType("int");
//            MethodSignature constructor = identifierFactory.getMethodSignature(
//                    arrayClass,
//                    "<init>",
//                    VoidType.getInstance(),
//                    List.of(intType));
//
//            Type objectType = identifierFactory.getType(Object.class.getCanonicalName());
//            MethodSignature addMethod = identifierFactory.getMethodSignature(
//                    listClass,
//                    "add",
//                    VoidType.getInstance(),
//                    List.of(intType, objectType));
//
//            MethodSignature getMethod = identifierFactory.getMethodSignature(
//                    listClass,
//                    "get",
//                    objectType,
//                    List.of(intType));
//
//            arrayMap = new ArrayMap();
//            arrayMap.setClassType(arrayClass);
//            arrayMap.setInitMethod(constructor);
//            arrayMap.setAddByIndexMethod(addMethod);
//            arrayMap.setGetByIndexMethod(getMethod);
//        }
//    }
//}
