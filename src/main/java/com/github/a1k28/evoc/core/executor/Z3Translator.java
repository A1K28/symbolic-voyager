package com.github.a1k28.evoc.core.executor;

import com.github.a1k28.evoc.core.executor.struct.*;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.*;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.visitor.AbstractExprVisitor;
import sootup.core.types.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class Z3Translator {
    private static Context ctx = null;
    private final SPath sPath;
    private final SStack symbolicVarStack;
    private static Map<String, BiFunction<AbstractInvokeExpr, List<Expr>, Expr>> methodModels;

    static {
        methodModels = new HashMap<>();
        // Register known method models
        methodModels.put("<java.lang.String: boolean equals(java.lang.Object)>", (invoke, args) ->
                ctx.mkEq(args.get(0), args.get(1)));
        methodModels.put("<java.lang.String: int length()>", (invoke, args) ->
                ctx.mkLength(args.get(0)));
        methodModels.put("<sootup.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String)>", (invoke, args) ->
                ctx.mkConcat(args.get(0), ctx.mkString(args.get(1).getString())));
    }

    Z3Translator(SPath sPath, SStack symbolicVarStack) {
        this.sPath = sPath;
        this.symbolicVarStack = symbolicVarStack;
    }

    static Solver makeSolver() {
        return ctx.mkSolver();
    }

    static void initZ3() {
        // Initialize Z3
        if (ctx == null) {
            ctx = new Context();
        }
    }

    static void closeZ3() {
        ctx.close();
    }

    Expr mkEq(Expr expr, boolean val) {
        return ctx.mkEq(expr, ctx.mkBool(val));
    }

    Expr mkEq(Expr expr, Expr val) {
        return ctx.mkEq(expr, val);
    }

    Expr mkExpr(String name, Type type) {
        Sort sort = translateType(type);
        return ctx.mkConst(name, sort);
    }

    SAssignment translateAndWrapValues(Value value1, Value value2, VarType varType) {
        SExpr left;
        if (value1 instanceof Local) {
            left = new SExpr(getSymbolicValue(value1, value2.getType(), varType));
        } else {
            left = new SExpr(translateValue(value1, varType));
        }

        SExpr right;
        if (value2 instanceof AbstractInvokeExpr invoke) {
            right = wrapMethodCall(invoke);
        } else {
            right = new SExpr(translateValue(value2, varType));
        }

        return new SAssignment(left, right);
    }

    Expr handleMethodCall(SMethodExpr methodExpr, VarType varType) {
        String methodSignature = methodExpr.getInvokeExpr().getMethodSignature().toString();

        List<Expr> args = methodExpr.getArgs().stream()
                .map(e -> this.translateValue(e, varType))
                .collect(Collectors.toList());

        BiFunction<AbstractInvokeExpr, List<Expr>, Expr> methodModel = methodModels.get(methodSignature);
        if (methodModel != null) {
            return methodModel.apply(methodExpr.getInvokeExpr(), args);
        } else {
            return handleUnknownMethod(methodExpr.getInvokeExpr(), args);
        }
    }

    Expr translateCondition(Value condition, VarType varType) {
        if (condition instanceof AbstractConditionExpr exp) {
            AssignmentExprHolder holder = translateValues(exp.getOp1(), exp.getOp2(), varType);
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

    private SMethodExpr wrapMethodCall(AbstractInvokeExpr invoke) {
        String methodSignature = invoke.getMethodSignature().toString();

        List<Value> args = new ArrayList<>();
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            args.add(i.getBase());
        for (Value arg : invoke.getArgs())
            args.add(arg);
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs())
                args.add(arg);

        if (methodModels.containsKey(methodSignature)) {
            return new SMethodExpr(invoke, args, false);
        } else {
            return new SMethodExpr(invoke, args, true);
        }
    }

    private AssignmentExprHolder translateValues(Value value1, Value value2, VarType varType) {
        Expr left;
        if (value1 instanceof Local) {
            left = getSymbolicValue(value1, value2.getType(), varType);
        } else {
            left = translateValue(value1, varType);
        }
        Expr right = translateValue(value2, varType);
        return new AssignmentExprHolder(left, right);
    }

    private Expr translateValue(Value value, VarType varType) {
        if (value instanceof Local) {
            return getSymbolicValue(value, varType);
        }
        if (value instanceof JFieldRef) {
            return getSymbolicValue(value, varType);
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
            return handleMethodCall(abstractInvoke, varType);
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
                return ctx.mkLength(translateValue(value, varType));
            if (unop instanceof JNegExpr)
                return ctx.mkNot(translateValue(value, varType));
        }
        if (value instanceof AbstractExprVisitor visitor) {
            // handle
        }
        if (value instanceof AbstractBinopExpr binop) {
            AssignmentExprHolder holder = translateValues(binop.getOp1(), binop.getOp2(), varType);
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

    private Expr handleMethodCall(AbstractInvokeExpr invoke, VarType varType) {
        String methodSignature = invoke.getMethodSignature().toString();

        List<Expr> args = new ArrayList<>();
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            args.add(translateValue(i.getBase(), varType));
        for (Value arg : invoke.getArgs())
            args.add(translateValue(arg, varType));
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs())
                args.add(translateValue(arg, varType));

        BiFunction<AbstractInvokeExpr, List<Expr>, Expr> methodModel = methodModels.get(methodSignature);
        if (methodModel != null) {
            return methodModel.apply(invoke, args);
        } else {
            return handleUnknownMethod(invoke, args);
        }
    }

    private Expr handleUnknownMethod(AbstractInvokeExpr invoke, List<Expr> args) {
//        if (invoke.getMethodSignature().getDeclClassType().toString().equals(classname)) {
//        }

        throw new RuntimeException("Unknown method: " + invoke.getMethodSignature());

//        // strategy 1: create a fresh symbolic variable
//        Sort returnSort = translateType(invoke.getType());
//        String freshVarName = "result_" + invoke.getMethodSignature().getSubSignature().getName() + "_" + System.identityHashCode(invoke);
//        Expr result = ctx.mkConst(freshVarName, returnSort);
//
//        // strategy 2: add constraints based on method properties
//        if (invoke.getMethodSignature().getSubSignature().getName().startsWith("get")) {
//            // Getter methods typically return a non-null value
//            if (returnSort instanceof SeqSort || returnSort.toString().equals("Object")) {
//                solver.add(ctx.mkNot(ctx.mkEq(result, mkNull(returnSort))));
//            }
//        } else if (invoke.getMethodSignature().getDeclClassType().toString().equals(classname)) {
////            return callMethod(invoke, args);
//        } else {
//            // TODO: mock and save param as field/param
//            log.warn("Mocking unknown method: " + invoke.getMethodSignature() +". If you would like to" +
//                    " disable this behaviour, please specify the flag --no-mocks");
//            return getSymbolicValue(invoke, invoke.getType());
////            throw new RuntimeException("Unknown method: " + invoke.getMethodSignature());
//        }
//
//        return result;
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

    private Expr getSymbolicValue(Value value, VarType varType) {
        return getSymbolicVar(value, null, varType).getExpr();
    }

    private Expr getSymbolicValue(Value value, Type type, VarType varType) {
        return getSymbolicVar(value, type, varType).getExpr();
    }

    SVar getSymbolicVarStrict(Value value) {
        return getSymbolicVar(value, null, null);
    }

    SVar updateSymbolicVariable(Value variable, Expr expression, VarType varType) {
        if (variable != null && expression != null) {
            String name = getValueName(variable);
            if (name.startsWith("this.")) name = name.substring(5);
            if (sPath.getFields().contains(name)) varType = VarType.FIELD;
            return symbolicVarStack.add(name, variable, expression, varType);
        }
        return null;
    }

    private SVar getSymbolicVar(Value value, Type type, VarType varType) {
        String key = getValueName(value);
        Optional<SVar> optional = symbolicVarStack.get(key);
        return optional.orElseGet(() -> saveSymbolicVar(value, type, varType));
    }

    SVar saveSymbolicVar(Value value, Type type, VarType varType) {
        String name = getValueName(value);
        if (type == null) type = value.getType();
        Expr expr = mkExpr(name, type);
        return updateSymbolicVariable(value, expr, varType);
    }

    String getValueName(Value value) {
        return value.toString();
    }

    private Expr mkNull(SStack stack, Sort sort) {
        return ctx.mkConst("null_" + sort.toString(), sort);
    }
}
