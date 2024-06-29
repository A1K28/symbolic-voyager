package com.github.a1k28.evoc.core.symbex;

import com.github.a1k28.evoc.core.symbex.struct.SNode;
import com.github.a1k28.evoc.core.symbex.struct.SPath;
import com.github.a1k28.evoc.core.symbex.struct.SType;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolicPathGenerator {
    private static Context ctx;
    private static Solver solver;
    private static Map<Value, Expr> symbolicVariables;

    public static void analyzeSymbolicPaths(String className, String methodName) {
        // Initialize Soot
        soot.G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(System.getProperty("java.class.path"));
        Options.v().set_output_format(Options.output_format_none);
        SootClass sootClass = Scene.v().loadClassAndSupport(className);
        sootClass.setApplicationClass();
        Scene.v().loadNecessaryClasses();

        // Get the method
        SootMethod method = sootClass.getMethodByName(methodName);
        Body body = method.retrieveActiveBody();

        // Generate CFG
        UnitGraph cfg = new ExceptionalUnitGraph(body);

        // Initialize Z3
        ctx = new Context();

        // Find all paths
        SPath sPath = createFlowDiagram(cfg);

//        sPath.print();

        // Analyze each path
        List<SNode> path;
        while (!(path = sPath.getNextPath()).isEmpty()) {
            analyzePath(sPath, path);
        }
    }

    private static SPath createFlowDiagram(UnitGraph cfg) {
        SPath sPath = new SPath();
        Unit start = cfg.getHeads().get(0);
        dfs(cfg, start, sPath, sPath.getRoot());
        return sPath;
    }

    private static void dfs(UnitGraph cfg, Unit current, SPath sPath, SNode parent) {
        SNode node = sPath.createNode(current);
        parent.addChild(node);

        if (!cfg.getTails().contains(current)) {
            List<Unit> succs = cfg.getSuccsOf(current);
            if (node.getType() == SType.BRANCH) {
                if (succs.size() != 2) throw new RuntimeException("Invalid branch successor size");
                SNode node2 = sPath.createNode(current);
                parent.addChild(node2);

                node.setType(SType.BRANCH_FALSE);
                node2.setType(SType.BRANCH_TRUE);

                dfs(cfg, succs.get(0), sPath, node);
                dfs(cfg, succs.get(1), sPath, node2);
            } else {
                for (Unit succ : succs) {
                    if (!node.containsParent(succ)) {
                        dfs(cfg, succ, sPath, node);
                    }
                }
            }
        }
    }

    private static void analyzePath(SPath sPath, List<SNode> path) {
        solver = ctx.mkSolver();
        symbolicVariables = new HashMap<>();

        // prune
        if (path.isEmpty()) {
            System.out.println("Path is unsatisfiable");
            return;
        }

        for (SNode node : path) {
            Unit unit = node.getUnit();
            if (node.getType() == SType.BRANCH_TRUE
                    || node.getType() == SType.BRANCH_FALSE) {
                IfStmt ifStmt = (IfStmt) unit;
                Value condition = ifStmt.getCondition();
                Expr z3Condition = translateCondition(condition);
                solver.add(ctx.mkEq(z3Condition, ctx.mkBool(
                        node.getType() == SType.BRANCH_TRUE)));
            } else if (node.getType() == SType.ASSIGNMENT) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                Expr leftExpr = translateValue(leftOp);
                Expr rightExpr = translateValue(rightOp);
                solver.add(ctx.mkEq(leftExpr, rightExpr));
            }
            // Handle other types of statements as needed

            if (solver.check() != Status.SATISFIABLE) {
                node.setSatisfiable(false);
                System.out.println("Path is unsatisfiable");
                return;
            }
        }

        // Check satisfiability
        System.out.println("Path is satisfiable");
        Model model = solver.getModel();
        for (Map.Entry<Value, Expr> entry : symbolicVariables.entrySet()) {
            ParameterRef parameterRef = sPath.getNameToParamIdx().getOrDefault(entry.getValue().toString(), null);
            if (parameterRef != null) {
                System.out.println(entry.getKey() + " = " + model.eval(entry.getValue(), false) + " " + parameterRef);
            }
        }
        System.out.println();
    }

    private static Expr translateCondition(Value condition) {
        if (condition instanceof EqExpr eq) {
            return ctx.mkEq(translateValue(eq.getOp1()), translateValue(eq.getOp2()));
        } else if (condition instanceof NeExpr ne) {
            return ctx.mkNot(ctx.mkEq(translateValue(ne.getOp1()), translateValue(ne.getOp2())));
        } else if (condition instanceof GtExpr gt) {
            return ctx.mkGt(translateValue(gt.getOp1()), translateValue(gt.getOp2()));
        } else if (condition instanceof GeExpr ge) {
            return ctx.mkGe(translateValue(ge.getOp1()), translateValue(ge.getOp2()));
        } else if (condition instanceof LtExpr lt) {
            return ctx.mkLt(translateValue(lt.getOp1()), translateValue(lt.getOp2()));
        } else if (condition instanceof LeExpr le) {
            return ctx.mkLe(translateValue(le.getOp1()), translateValue(le.getOp2()));
        }
        // Handle other types of conditions
        return ctx.mkBool(true);
    }

    private static Expr translateValue(Value value) {
        if (value instanceof IntConstant) {
            return ctx.mkInt(((IntConstant) value).value);
        } else if (value instanceof Local) {
            return getSymbolicValue(value);
        } else if (value instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) value;
            Expr left = translateValue(binop.getOp1());
            Expr right = translateValue(binop.getOp2());
            if (binop instanceof AddExpr) {
                return ctx.mkAdd(left, right);
            } else if (binop instanceof SubExpr) {
                return ctx.mkSub(left, right);
            } else if (binop instanceof MulExpr) {
                return ctx.mkMul(left, right);
            } else if (binop instanceof DivExpr) {
                return ctx.mkDiv(left, right);
            } else if (binop instanceof RemExpr) {
                return ctx.mkMod(left, right);
            }
            // Handle other binary operations
        }
        // Handle other types of values
        return ctx.mkIntConst(value.toString());
    }

    private static Expr getSymbolicValue(Value value) {
        if (!symbolicVariables.containsKey(value)) {
            if (value instanceof IntConstant) {
                symbolicVariables.put(value, ctx.mkInt(((IntConstant) value).value));
            } else if (value instanceof Local) {
                symbolicVariables.put(value, ctx.mkIntConst(value.toString()));
            } else {
                // Handle other types as needed
                symbolicVariables.put(value, ctx.mkIntConst(value.toString()));
            }
        }
        return symbolicVariables.get(value);
    }

    public static void main(String[] args) {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
        analyzeSymbolicPaths("com.github.a1k28.Stack", "test");
    }
}