package com.github.a1k28.evoc.core.symbex;

import com.github.a1k28.evoc.core.symbex.struct.SNode;
import com.github.a1k28.evoc.core.symbex.struct.SPath;
import com.github.a1k28.evoc.core.symbex.struct.SType;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import sootup.core.Project;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.language.JavaLanguage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolicPathGenerator {
    private static Context ctx;
    private static Map<Value, Expr> symbolicVariables;

    public static void analyzeSymbolicPaths(String className, String methodName) {
        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        JavaLanguage language = new JavaLanguage(17);

        Project project = JavaProject.builder(language)
                .addInputLocation(inputLocation).build();

        ClassType classType =
                project.getIdentifierFactory().getClassType(className);

        View<?> view = project.createView();

        SootClass<JavaSootClassSource> sootClass =
                (SootClass<JavaSootClassSource>) view.getClass(classType).get();

        // Get the method
        SootMethod method = sootClass.getMethods().stream()
                .filter(e -> e.getName().equals(methodName)).findFirst().get();
        Body body = method.getBody();

        // Generate CFG
        StmtGraph<?> cfg = body.getStmtGraph();

        // Initialize Z3
        ctx = new Context();

        // Find all paths
        SPath sPath = createFlowDiagram(cfg);

//        sPath.print();

        // Analyze each path
        List<SNode> path;
        while ((path = sPath.getNextPath()) != null) {
            if (path.isEmpty()) continue;
            analyzePath(sPath, path);
        }
    }

    private static SPath createFlowDiagram(StmtGraph<?> cfg) {
        SPath sPath = new SPath();
        Stmt start = cfg.getStartingStmt();
        dfs(cfg, start, sPath, sPath.getRoot());
        return sPath;
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

    private static void analyzePath(SPath sPath, List<SNode> path) {
        Solver solver = ctx.mkSolver();
        symbolicVariables = new HashMap<>();

        for (SNode node : path) {
            Stmt unit = node.getUnit();
            if (node.getType() == SType.BRANCH_TRUE
                    || node.getType() == SType.BRANCH_FALSE) {
                JIfStmt ifStmt = (JIfStmt) unit;
                Value condition = ifStmt.getCondition();
                Expr z3Condition = translateCondition(condition);
                solver.add(ctx.mkEq(z3Condition, ctx.mkBool(
                        node.getType() == SType.BRANCH_TRUE)));
            } else if (node.getType() == SType.ASSIGNMENT) {
                JAssignStmt assignStmt = (JAssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                Expr rightExpr = translateValue(rightOp);
                updateSymbolicVariable(leftOp, rightExpr);
            }
            // Handle other types of statements as needed

            // Check satisfiability
            if (solver.check() != Status.SATISFIABLE) {
                node.setSatisfiable(false);
                System.out.println("Path is unsatisfiable");
                return;
            }
        }

        System.out.println("Path is satisfiable");

//        for (SNode node : path)
//            System.out.println(node);

        Model model = solver.getModel();
        for (Map.Entry<Value, Expr> entry : symbolicVariables.entrySet()) {
            JParameterRef parameterRef = sPath.getNameToParamIdx().getOrDefault(entry.getValue().toString(), null);
            if (parameterRef != null) {
                System.out.println(entry.getKey() + " = " + model.eval(entry.getValue(), false) + " " + parameterRef);
            }
        }
        System.out.println();
    }

    private static Expr translateCondition(Value condition) {
        if (condition instanceof JEqExpr eq) {
            return ctx.mkEq(translateValue(eq.getOp1()), translateValue(eq.getOp2()));
        } else if (condition instanceof JNeExpr ne) {
            return ctx.mkNot(ctx.mkEq(translateValue(ne.getOp1()), translateValue(ne.getOp2())));
        } else if (condition instanceof JGtExpr gt) {
            return ctx.mkGt(translateValue(gt.getOp1()), translateValue(gt.getOp2()));
        } else if (condition instanceof JGeExpr ge) {
            return ctx.mkGe(translateValue(ge.getOp1()), translateValue(ge.getOp2()));
        } else if (condition instanceof JLtExpr lt) {
            return ctx.mkLt(translateValue(lt.getOp1()), translateValue(lt.getOp2()));
        } else if (condition instanceof JLeExpr le) {
            return ctx.mkLe(translateValue(le.getOp1()), translateValue(le.getOp2()));
        }
        // Handle other types of conditions
        return ctx.mkBool(true);
    }

    private static Expr translateValue(Value value) {
        if (value instanceof IntConstant) {
            return ctx.mkInt(((IntConstant) value).getValue());
        } else if (value instanceof Local) {
            return getSymbolicValue(value);
        } else if (value instanceof AbstractBinopExpr) {
            AbstractBinopExpr binop = (AbstractBinopExpr) value;
            Expr left = translateValue(binop.getOp1());
            Expr right = translateValue(binop.getOp2());
            if (binop instanceof JAddExpr) {
                return ctx.mkAdd(left, right);
            } else if (binop instanceof JSubExpr) {
                return ctx.mkSub(left, right);
            } else if (binop instanceof JMulExpr) {
                return ctx.mkMul(left, right);
            } else if (binop instanceof JDivExpr) {
                return ctx.mkDiv(left, right);
            } else if (binop instanceof JRemExpr) {
                return ctx.mkMod(left, right);
            }
            // Handle other binary operations
        }
        // Handle other types of values
        return ctx.mkIntConst(value.toString());
    }

    private static void updateSymbolicVariable(Value variable, Expr expression) {
        symbolicVariables.put(variable, expression);
    }

    private static Expr getSymbolicValue(Value value) {
        if (!symbolicVariables.containsKey(value)) {
            if (value instanceof IntConstant) {
                symbolicVariables.put(value, ctx.mkInt(((IntConstant) value).getValue()));
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