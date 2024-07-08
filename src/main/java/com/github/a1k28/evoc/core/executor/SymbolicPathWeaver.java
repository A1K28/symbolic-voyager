package com.github.a1k28.evoc.core.executor;

import com.github.a1k28.evoc.core.executor.struct.*;
import com.github.a1k28.evoc.helper.Logger;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.java.core.JavaSootClassSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.a1k28.evoc.core.executor.Z3Translator.*;
import static com.github.a1k28.evoc.helper.SootHelper.getSootClass;
import static com.github.a1k28.evoc.helper.SootHelper.getSootMethod;

public class SymbolicPathWeaver {
    private static final Logger log = Logger.getInstance(SymbolicPathWeaver.class);
    private final String classname;
    private static Solver solver = null;
    private final SStack symbolicVarStack = new SStack();
    private final Z3Translator z3t;

    public SymbolicPathWeaver(String classname) {
        this.classname = classname;
        this.z3t = new Z3Translator(symbolicVarStack);
    }

    public void analyzeSymbolicPaths(String methodName) throws ClassNotFoundException {
        initZ3();
        solver = makeSolver();
        SPath sPath = createPath(methodName);
        analyzePaths(sPath, sPath.getRoot());
        closeZ3();
    }

    public List<SVar> getPossibleReturnValues(String methodName, List<SVar> args) throws ClassNotFoundException {
        SPath sPath = createPath(methodName);
        args.forEach(symbolicVarStack::add);
        List<SVar> sVars = new ArrayList<>();
        analyzeReturnValues(sPath.getRoot(), sVars);
        return sVars;
    }

    private SPath createPath(String methodName) throws ClassNotFoundException {
        // Find all paths
        SootClass<JavaSootClassSource> sootClass = getSootClass(classname);

        SootMethod method = getSootMethod(sootClass, methodName);
        Body body = method.getBody();

        SPath sPath = new SPath();
        sPath.addFields(sootClass);

        // Generate CFG
        StmtGraph<?> cfg = body.getStmtGraph();
        createFlowDiagram(sPath, cfg);

        log.debug("Printing method: " + methodName);
        sPath.print();
        return sPath;
    }

    public static void createFlowDiagram(SPath sPath, StmtGraph<?> cfg) {
        Stmt start = cfg.getStartingStmt();
        dfs(cfg, start, sPath, sPath.getRoot());
    }

    private static void dfs(StmtGraph<?> cfg, Stmt current, SPath sPath, SNode parent) {
        SNode node = sPath.createNode(current);
        parent.addChild(node);

        if (!cfg.getTails().contains(current)) {
            List<Stmt> succs = cfg.getAllSuccessors(current);
            if (node.getType() == SType.BRANCH) {
                if (succs.size() != 2) throw new RuntimeException("Invalid branch successor size");
                SNode node2 = sPath.createNode(current);
                parent.addChild(node2);

                node.setType(SType.BRANCH_FALSE);
                node2.setType(SType.BRANCH_TRUE);

                dfs(cfg, succs.get(0), sPath, node);
                dfs(cfg, succs.get(1), sPath, node2);
            } else {
                for (Stmt succ : succs) {
                    if (!node.containsParent(succ)) {
                        dfs(cfg, succ, sPath, node);
                    }
                }
            }
        }
    }

    private void analyzeReturnValues(SNode node, List<SVar> sVars) throws ClassNotFoundException {
        symbolicVarStack.push();

        SType type = null;

        if (node.getType() == SType.ASSIGNMENT) {
            type = handleAssignment(node, true, sVars);
        }

        if (node.getType() == SType.RETURN) {
            JReturnStmt unit = (JReturnStmt) node.getUnit();
            sVars.add(symbolicVarStack.get(z3t.getValueKey(unit.getOp())).get());
        }

        if (SType.INVOKE != type) for (SNode child : node.getChildren()) analyzeReturnValues(child, sVars);

        symbolicVarStack.pop();
    }

    private void analyzePaths(SPath sPath, SNode node) throws ClassNotFoundException {
        solver.push();
        symbolicVarStack.push();

        SType type = null;

        // handle node types
        if (node.getType() == SType.BRANCH_TRUE
                || node.getType() == SType.BRANCH_FALSE) {
            handleBranch(node);
        }
        if (node.getType() == SType.ASSIGNMENT) {
            type = handleAssignment(node, false, sPath);
        }
        if (node.getType() == SType.INVOKE) {
//                handleInvoke();
        }

        // check satisfiability
        if (solver.check() != Status.SATISFIABLE) {
            log.warn("Path is unsatisfiable\n");
        } else if (SType.INVOKE != type) {
            // recurse for children
            if (!node.getChildren().isEmpty()) {
                for (SNode child : node.getChildren())
                    analyzePaths(sPath, child);
            } else {
                handleSatisfiability(sPath);
            }
        }

        solver.pop();
        symbolicVarStack.pop();
    }

    private void handleBranch(SNode node) {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        Expr z3Condition = z3t.translateCondition(condition);
        solver.add(z3t.mkEq(z3Condition, node.getType() == SType.BRANCH_TRUE));
    }

    private SType handleAssignment(SNode node, boolean isInnerCall, Object... args)
            throws ClassNotFoundException {
        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        List<SVar> returnValues = handleAssignment(node);

        for (SVar sVar : returnValues) {
            if (sVar.getType() == VarType.PARAMETER) {
                symbolicVarStack.push();
                z3t.updateSymbolicVariable(leftOp, sVar.getExpr());
                if (isInnerCall) for (SNode child : node.getChildren())
                        analyzeReturnValues(child, (List<SVar>) args[0]);
                else for (SNode child : node.getChildren())
                        analyzePaths((SPath) args[0], child);
                symbolicVarStack.pop();
            }
        }

        if (!returnValues.isEmpty()) return SType.INVOKE;
        return SType.ASSIGNMENT;
    }

    private List<SVar> handleAssignment(SNode node) throws ClassNotFoundException {
        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        SAssignment holder = z3t.translateAndWrapValues(leftOp, rightOp);

        if (holder.getRight().getSType() == SType.INVOKE) {
            SMethodExpr methodExpr = holder.getRight().asMethod();
            if (methodExpr.isUnknown()) {
                return returnPermutations(methodExpr);
            } else {
                z3t.updateSymbolicVariable(leftOp,
                        z3t.handleMethodCall(holder.getRight().asMethod()));
            }
        } else {
            z3t.updateSymbolicVariable(leftOp, holder.getRight().getExpr());
        }
        return Collections.emptyList();
    }

    private List<SVar> returnPermutations(SMethodExpr methodExpr) throws ClassNotFoundException {
        List<SVar> args = methodExpr.getInvokeExpr().getArgs().stream()
                .map(z3t::getSymbolicVar)
                .map(SVar::renew)
                .toList();
        String sig = methodExpr.getInvokeExpr().getMethodSignature().getSubSignature().toString();
        return new SymbolicPathWeaver(classname).getPossibleReturnValues(sig, args);
    }

    private void handleSatisfiability(SPath sPath) {
        // if tail
        log.info("Path is satisfiable");
        Model model = solver.getModel();

        for (SVar var : symbolicVarStack.getAll()) {
            SParam sParam = sPath.getParam(var.getName());
            if (sParam != null && var.isOriginal()) {
                Object evaluated = model.eval(var.getExpr(), true);
                log.debug(var.getValue() + " = " + evaluated + " " + sParam + " " + var.isOriginal());
            }
        }
        log.empty();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
        new SymbolicPathWeaver("com.github.a1k28.Stack").analyzeSymbolicPaths( "test_method_call");
    }
}