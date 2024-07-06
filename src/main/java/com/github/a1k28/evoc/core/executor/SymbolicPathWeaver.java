package com.github.a1k28.evoc.core.executor;

import com.github.a1k28.evoc.core.executor.struct.*;
import com.github.a1k28.evoc.helper.Logger;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.*;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractExprVisitor;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.*;
import sootup.java.core.JavaSootClassSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.github.a1k28.evoc.helper.SootHelper.*;

public class SymbolicPathWeaver {
    private static final Logger log = Logger.getInstance(SymbolicPathWeaver.class);
    private Solver solver;
    private Context ctx;
    private Map<String, SVar> symbolicVariables;
    private final Map<String, BiFunction<AbstractInvokeExpr, List<Expr>, Expr>> methodModels;

    public SymbolicPathWeaver() {
        methodModels = new HashMap<>();
        // Register known method models
        methodModels.put("<java.lang.String: boolean equals(java.lang.Object)>", (invoke, args) ->
                ctx.mkEq(args.get(0), args.get(1)));
        methodModels.put("<java.lang.String: int length()>", (invoke, args) ->
                ctx.mkLength(args.get(0)));
        methodModels.put("<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String)>", (invoke, args) ->
                ctx.mkConcat(args.get(0), ctx.mkString(args.get(1).getString())));
    }

    public void analyzeSymbolicPaths(String className, String methodName) throws ClassNotFoundException {
        // Initialize Z3
        ctx = new Context();
        solver = ctx.mkSolver();
        symbolicVariables = new HashMap<>();

        // Find all paths
        SootClass<JavaSootClassSource> sootClass = getSootClass(className);

        SootMethod method = getSootMethod(sootClass, methodName);
        Body body = method.getBody();

        SPath sPath = new SPath();
        sPath.addFields(sootClass);

        // Generate CFG
        StmtGraph<?> cfg = body.getStmtGraph();
        createFlowDiagram(sPath, cfg);

        sPath.print();

        analyzePaths(sPath, sPath.getRoot());

        ctx.close();
    }

    private void analyzePaths(SPath sPath, SNode node) {
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
            log.warn("Path is unsatisfiable\n");
        } else {
            // recurse for children
            if (!node.getChildren().isEmpty()) {
                for (SNode child : node.getChildren())
                    analyzePaths(sPath, child);
            } else {
                // if tail
                log.focus("Path is satisfiable");
                Model model = solver.getModel();

                for (Map.Entry<String, SVar> entry : symbolicVariables.entrySet()) {
                    SParam sParam = sPath.getParam(entry.getValue().getName());
                    if (sParam != null && entry.getValue().isOriginal()) {
                        Object evaluated = model.eval(entry.getValue().getExrp(), true);
                        log.info(entry.getValue().getValue() + " = " + evaluated + " " + sParam + " " + entry.getValue().isOriginal());
                    }
                }
                log.empty();
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
        }
        if (value instanceof JFieldRef) {
            return getSymbolicValue(value, value.getType());
        }
        if (value instanceof IntConstant v) {
            return ctx.mkInt(v.getValue());
        }
        if (value instanceof BooleanConstant v) {
            return ctx.mkBool(v.toString().equals("1"));
        }
        if (value instanceof DoubleConstant v) {
            return ctx.mkFP(v.getValue(), ctx.mkFPSort64());
        }
        if (value instanceof FloatConstant v) {
            return ctx.mkFP(v.getValue(), ctx.mkFPSort64());
        }
        if (value instanceof LongConstant v) {
            return ctx.mkInt(v.getValue());
        }
        if (value instanceof StringConstant v) {
            return ctx.mkString(v.getValue().replaceFirst("\u0001", ""));
        }
        if (value instanceof AbstractInvokeExpr abstractInvoke) {
            return handleMethodCall(abstractInvoke);
        }
        if (value instanceof JNewExpr) {
            return ctx.mkConst(value.toString(), ctx.mkUninterpretedSort(value.getType().toString()));
        }
        if (value instanceof JArrayRef) {
            // Create an integer sort for array indices
            IntSort intSort = ctx.getIntSort();

            // Create an uninterpreted sort for array elements
            Sort elementSort = ctx.mkUninterpretedSort(value.getType().toString());

            return ctx.mkArrayConst(value.toString(), intSort, elementSort);
        }
        if (value instanceof AbstractUnopExpr unop) {
            if (value instanceof JLengthExpr)
                return ctx.mkLength(translateValue(value));
            if (unop instanceof JNegExpr)
                return ctx.mkNot(translateValue(value));
        }
        if (value instanceof AbstractExprVisitor visitor) {
            // handle
        }
        if (value instanceof AbstractBinopExpr binop) {
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

//        return null;
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
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            args.add(translateValue(i.getBase()));
        for (Value arg : invoke.getArgs())
            args.add(translateValue(arg));
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs())
                args.add(translateValue(arg));

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
        String key = getValueKey(value);
        if (!symbolicVariables.containsKey(key)) {
            symbolicVariables.put(key, new SVar(key, value, mkExpr(value, type), true));
        }
        return symbolicVariables.get(key).getExrp();
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
        if (variable != null && expression != null) {
            String key = getValueKey(variable);
            boolean isOriginal = true;
            if (symbolicVariables.containsKey(key) && symbolicVariables.get(key).isOriginal()){
                symbolicVariables.put(key+"_ORIGINAL", symbolicVariables.get(key));
                isOriginal = false;
            }
            symbolicVariables.put(key, new SVar(key, variable, expression, isOriginal));
        }
    }

    private String getValueKey(Value value) {
        return value.toString();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
        new SymbolicPathWeaver().analyzeSymbolicPaths("com.github.a1k28.Stack", "test_string");
    }
}