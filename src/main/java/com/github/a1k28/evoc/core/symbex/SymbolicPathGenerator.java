package com.github.a1k28.evoc.core.symbex;

import com.github.a1k28.evoc.core.symbex.struct.SNode;
import com.github.a1k28.evoc.core.symbex.struct.SPath;
import com.github.a1k28.evoc.core.symbex.struct.SType;
import com.github.a1k28.evoc.core.symbex.struct.AssignmentExprHolder;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import sootup.core.Project;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.*;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractExprVisitor;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.*;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.language.JavaLanguage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class SymbolicPathGenerator {
    private Solver solver;
    private Context ctx;
    private Map<Value, Expr> symbolicVariables;
    private final Map<String, BiFunction<AbstractInvokeExpr, List<Expr>, Expr>> methodModels;

    public SymbolicPathGenerator() {
        methodModels = new HashMap<>();
        // Register known method models
        methodModels.put("<java.lang.String: boolean equals(java.lang.Object)>", (invoke, args) ->
                ctx.mkEq(args.get(0), args.get(1)));
        methodModels.put("<java.lang.String: int length()>", (invoke, args) ->
                ctx.mkLength(args.get(0)));
    }

    public void analyzeSymbolicPaths(String className, String methodName) throws ClassNotFoundException {
        int javaVersion = getJavaVersion(Class.forName(className));

        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        JavaLanguage language = new JavaLanguage(javaVersion);

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

        sPath.print();

        solver = ctx.mkSolver();
        symbolicVariables = new HashMap<>();
        analyzePaths(sPath, sPath.getRoot(), 1);
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

    private void analyzePaths(SPath sPath, SNode node, int level) {
        solver.push();

        // handle node types
        if (node.getType() != SType.ROOT) {
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
                AssignmentExprHolder holder = translateValues(leftOp, rightOp);
                updateSymbolicVariable(leftOp, holder.getRight());
            }
        }

        // check satisfiability
        if (solver.check() != Status.SATISFIABLE) {
            System.out.println("Path is unsatisfiable");
            System.out.println();
        } else {
            // recurse for children
            if (!node.getChildren().isEmpty()) {
                for (SNode child : node.getChildren())
                    analyzePaths(sPath, child, level + 1);
            } else {
                // if tail
                System.out.println("Path is satisfiable");
                Model model = solver.getModel();

                for (Map.Entry<Value, Expr> entry : symbolicVariables.entrySet()) {
                    JParameterRef parameterRef = sPath.getNameToParamIdx().getOrDefault(entry.getKey().toString(), null);
                    if (parameterRef != null) {
                        System.out.println(entry.getKey() + " = " + model.eval(entry.getValue(), false) + " " + parameterRef);
                    }
                }
                System.out.println();
            }
        }

        solver.pop();
    }

    private Expr translateCondition(Value condition) {
        if (condition instanceof AbstractConditionExpr exp) {
            AssignmentExprHolder holder = translateValues(exp.getOp1(), exp.getOp2());
            Expr e1 = holder.getLeft();
            Expr e2 = holder.getRight();
            if (e1.isBool() && e2.isInt()) {
                int val = ((IntNum) e2).getInt();
                e2 = ctx.mkBool(val == 1);
            } else if (e1.isInt() && e2.isBool()) {
                int val = ((IntNum) e1).getInt();
                e1 = ctx.mkBool(val == 1);
            }
            if (condition instanceof JEqExpr) {
                return ctx.mkEq(e1, e2);
            } else if (condition instanceof JNeExpr) {
                return ctx.mkNot(ctx.mkEq(e1, e2));
            } else if (condition instanceof JGtExpr) {
                return ctx.mkGt(e1, e2);
            } else if (condition instanceof JGeExpr) {
                return ctx.mkGe(e1, e2);
            } else if (condition instanceof JLtExpr) {
                return ctx.mkLt(e1, e2);
            } else if (condition instanceof JLeExpr) {
                return ctx.mkLe(e1, e2);
            }
        }
        throw new RuntimeException("Condition could not be translated: " + condition);
    }

    private AssignmentExprHolder translateValues(Value value1, Value value2) {
        Expr left;
        if (value1 instanceof Local) {
            left = getSymbolicValue(value1, value2.getType());
        } else {
            left = translateValue(value1);
        }
        Expr right = translateValue(value2);
        return new AssignmentExprHolder(left, right);
    }

    private Expr translateValue(Value value) {
        if (value instanceof Local) {
            return getSymbolicValue(value);
        } else if (value instanceof IntConstant v) {
            return ctx.mkInt(v.getValue());
        } else if (value instanceof BooleanConstant v) {
            return ctx.mkBool(v.toString().equals("1"));
        } else if (value instanceof DoubleConstant v) {
            return ctx.mkFP(v.getValue(), ctx.mkFPSort64());
        } else if (value instanceof FloatConstant v) {
            return ctx.mkFP(v.getValue(), ctx.mkFPSort64());
        } else if (value instanceof LongConstant v) {
            return ctx.mkInt(v.getValue());
        } else if (value instanceof StringConstant v) {
            return ctx.mkString(v.getValue());
        } else if (value instanceof AbstractInvokeExpr abstractInvoke) {
            return handleMethodCall(abstractInvoke);
        } else if (value instanceof AbstractUnopExpr unop) {
            if (value instanceof JLengthExpr)
                return ctx.mkLength(translateValue(value));
            if (unop instanceof JNegExpr)
                return ctx.mkNot(translateValue(value));
        } else if (value instanceof AbstractExprVisitor visitor) {
            // handle
        } else if (value instanceof AbstractBinopExpr binop) {
            AssignmentExprHolder holder = translateValues(binop.getOp1(), binop.getOp2());
            Expr left = holder.getLeft();
            Expr right = holder.getRight();
            if (binop instanceof JAddExpr)
                return ctx.mkAdd(left, right);
            if (binop instanceof JSubExpr)
                return ctx.mkSub(left, right);
            if (binop instanceof JMulExpr)
                return ctx.mkMul(left, right);
            if (binop instanceof JDivExpr)
                return ctx.mkDiv(left, right);
            if (binop instanceof JRemExpr)
                return ctx.mkMod(left, right);
            if (binop instanceof JAndExpr)
                return ctx.mkAnd(left, right);
            if (binop instanceof JOrExpr)
                return ctx.mkOr(left, right);
            if (binop instanceof JShlExpr)
                throw new RuntimeException("Invalid binop encountered: JShlExpr (shift left)");
            if (binop instanceof JShrExpr)
                throw new RuntimeException("Invalid binop encountered: JShrExpr (shift right)");
            if (binop instanceof JUshrExpr)
                throw new RuntimeException("Invalid binop encountered: JUshrExpr (unsigned shift right)");
            if (binop instanceof JXorExpr)
                return ctx.mkXor(left, right);
            if (binop instanceof JCmpExpr
                    || binop instanceof JCmpgExpr
                    || binop instanceof JCmplExpr
                    || binop instanceof JEqExpr
                    || binop instanceof JGeExpr
                    || binop instanceof JGtExpr
                    || binop instanceof JLeExpr
                    || binop instanceof JLtExpr
                    || binop instanceof JNeExpr)
                return translateConditionValue(binop, left, right);
            // handle other binary operations
        }
//        else if (value instanceof JCastExpr)
//            return ctx.mkMod(left, right);
//        else if (value instanceof JInstanceOfExpr)
//            return ctx.mkMod(left, right);
//        else if (value instanceof JNewArrayExpr)
//            return ctx.mkMod(left, right);
//        else if (value instanceof JNewExpr)
//            return ctx.mkMod(left, right);
//        else if (value instanceof JNewMultiArrayExpr)
//            return ctx.mkMod(left, right);
//        else if (value instanceof JPhiExpr)
//            return ctx.mkMod(left, right);

        throw new RuntimeException("Could not resolve type for: " + value);
    }

    private Expr translateConditionValue(AbstractBinopExpr binop, Expr e1, Expr e2) {
        if (binop instanceof JEqExpr)
            return ctx.mkEq(e1, e2);
        if (binop instanceof JNeExpr)
            return ctx.mkNot(ctx.mkEq(e1, e2));
        if (binop instanceof JGtExpr)
            return ctx.mkGt(e1, e2);
        if (binop instanceof JCmpgExpr || binop instanceof JGeExpr)
            return ctx.mkGe(e1, e2);
        if (binop instanceof JLtExpr)
            return ctx.mkLt(e1, e2);
        if (binop instanceof JCmplExpr || binop instanceof JLeExpr)
            return ctx.mkLe(e1, e2);
        throw new RuntimeException("Condition could not be translated: " + binop);
    }

    private Expr handleMethodCall(AbstractInvokeExpr invoke) {
        String methodSignature = invoke.getMethodSignature().toString();

        List<Expr> args = new ArrayList<>();
        if (invoke instanceof AbstractInstanceInvokeExpr i) {
            args.add(translateValue(i.getBase()));
        }
        for (Value arg : invoke.getArgs()) {
            args.add(translateValue(arg));
        }

        BiFunction<AbstractInvokeExpr, List<Expr>, Expr> methodModel = methodModels.get(methodSignature);
        if (methodModel != null) {
            return methodModel.apply(invoke, args);
        } else {
            return handleUnknownMethod(invoke, args);
        }
    }

    private Expr handleUnknownMethod(AbstractInvokeExpr invoke, List<Expr> args) {
        // strategy 1: create a fresh symbolic variable
        Sort returnSort = translateType(invoke.getType());
        String freshVarName = "result_" + invoke.getMethodSignature().getSubSignature().getName() + "_" + System.identityHashCode(invoke);
        Expr result = ctx.mkConst(freshVarName, returnSort);

        // strategy 2: add constraints based on method properties
        if (invoke.getMethodSignature().getSubSignature().getName().startsWith("get")) {
            // Getter methods typically return a non-null value
            if (returnSort instanceof SeqSort || returnSort.toString().equals("Object")) {
                solver.add(ctx.mkNot(ctx.mkEq(result, mkNull(returnSort))));
            }
        }

        return result;
    }

    private Expr getSymbolicValue(Value value) {
        return getSymbolicValue(value, null);
    }

    private Expr getSymbolicValue(Value value, Type type) {
        if (!symbolicVariables.containsKey(value)) {
            symbolicVariables.put(value, mkExpr(value, type));
        }
        return symbolicVariables.get(value);
    }

    private Expr mkExpr(Value value, Type type) {
        if (type == null) type = value.getType();
        Sort sort = translateType(type);
        return ctx.mkConst(value.toString(), sort);
    }

    private Sort translateType(Type type) {
        if (type instanceof PrimitiveType.BooleanType)
            return ctx.getBoolSort();
        if (type instanceof PrimitiveType.ByteType)
            return ctx.getIntSort();
        if (type instanceof PrimitiveType.ShortType)
            return ctx.getIntSort();
        if (type instanceof PrimitiveType.CharType)
            return ctx.mkCharSort();
        if (type instanceof PrimitiveType.IntType)
            return ctx.getIntSort();
        if (type instanceof PrimitiveType.LongType)
            return ctx.mkIntSort();
        if (type instanceof PrimitiveType.FloatType)
            return ctx.mkFPSort32();
        if (type instanceof PrimitiveType.DoubleType)
            return ctx.mkFPSort64();
        if (type instanceof ArrayType) {
            Sort elementSort = translateType(((ArrayType) type).getElementType());
            return ctx.mkArraySort(ctx.getIntSort(), elementSort);
        }
        if (type instanceof ReferenceType) {
            if (type.toString().equals(String.class.getName())) {
                return ctx.getStringSort();
            }
            // For other reference types, use an uninterpreted sort
            return ctx.mkUninterpretedSort("Object");
        }
        if (type instanceof VoidType)
            return ctx.mkUninterpretedSort("Void");

        // For any other types, use an uninterpreted sort
        return ctx.mkUninterpretedSort(type.toString());
    }

    private Expr mkNull(Sort sort) {
        return ctx.mkConst("null_" + sort.toString(), sort);
    }

    private void updateSymbolicVariable(Value variable, Expr expression) {
        symbolicVariables.put(variable, expression);
    }

    public static int getJavaVersion(Class<?> clazz) {
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(
                clazz.getName().replace('.', '/') + ".class");
             DataInputStream dis = new DataInputStream(is)) {

            dis.readInt(); // Skip magic number
            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            return mapVersionToJava(majorVersion, minorVersion);
        } catch (IOException e) {
            throw new RuntimeException("Error reading class file", e);
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

    public static void main(String[] args) throws ClassNotFoundException {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
        new SymbolicPathGenerator().analyzeSymbolicPaths("com.github.a1k28.Stack", "test_string");
    }
}