package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.symbolicexecutor.model.MethodPropagationType;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.struct.MethodModel;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.Expr;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.*;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.visitor.AbstractExprVisitor;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.*;

import java.lang.reflect.Method;
import java.util.*;

import static com.github.a1k28.evoc.helper.SootHelper.isConstructorCall;

public class Z3Translator {
    private static volatile Z3ExtendedContext ctx;
    private static final Logger log = Logger.getInstance(Z3Translator.class);

    public Z3Translator() {
        initZ3(true);
    }

    public static void initZ3(boolean force) {
        // Initialize Z3
        if (force) {
            ctx = null;
        }
        if (ctx == null) {
            synchronized (Z3Translator.class) {
                if (ctx == null) {
                    ctx = new Z3ExtendedContext();
                    log.info("Successfully initialized Z3 context");
                }
            }
        }
    }

    public static void close() {
        if (ctx != null) {
            synchronized (Z3Translator.class) {
                if (ctx != null) {
                    ctx.close();
                    ctx = null;
                    log.info("Successfully closed Z3 context");
                }
            }
        }
    }

    public static Z3ExtendedContext getContext() {
        return ctx;
    }

    public static boolean containsAssertion(Expr assertion) {
        return Arrays.asList(ctx.getSolver().getAssertions()).contains(assertion);
    }

    public static Expr mkNull() {
        return ctx.mkConst("null", SortType.NULL.value(ctx));
    }

    public IStack getStack() {
        return ctx;
    }

    public BoolExpr mkNot(Expr expr) {
        return ctx.mkNot(expr);
    }

    public BoolExpr mkEq(Expr expr, Expr val) {
        return ctx.mkEq(expr, val);
    }

    public Expr mkExpr(String name, Sort sort) {
        return ctx.mkFreshConst(name, sort);
    }

    public SExpr translateAndWrapValue(Value value, VarType varType, SMethodPath methodPath) {
        if (value instanceof AbstractInvokeExpr invoke) {
            return wrapMethodCall(invoke, null);
        } else if (value instanceof JNewExpr && CLIOptions.shouldPropagate(value.toString())) {
            return new SExpr(translateValue(value, varType, methodPath), SType.INVOKE_SPECIAL_CONSTRUCTOR);
        } else {
            return new SExpr(translateValue(value, varType, methodPath));
        }
    }

    public SAssignment translateAndWrapValues(
            Value value1, Value value2, VarType varType, SMethodPath methodPath) {
        SExpr right;
        if (value2 instanceof AbstractInvokeExpr invoke) {
            right = wrapMethodCall(invoke, translateValue(value1, varType, methodPath));
        } else {
            right = new SExpr(translateValue(value2, varType, methodPath));
        }

        SExpr left;
        if (value1 instanceof Local || value1 instanceof JFieldRef) {
            Sort sort;
            if (value2.getType().getClass() == UnknownType.class)
                sort = right.getExpr().getSort();
            else
                sort = translateType(value2.getType());
            left = new SExpr(getSymbolicValue(value1, sort, varType, methodPath));
        } else {
            left = new SExpr(translateValue(value1, varType, methodPath));
        }

        return new SAssignment(left, right);
    }

    public Expr callProverMethod(SMethodExpr methodExpr, VarType varType, SMethodPath methodPath) {
        MethodSignature methodSignature = methodExpr.getInvokeExpr().getMethodSignature();
        MethodModel methodModel = MethodModel.get(methodSignature).orElseThrow();

        List<Expr> args = new ArrayList<>();
        if (methodModel.hasBase() && methodExpr.getBase() != null)
            args.add(translateValue(methodExpr.getBase(), varType, methodPath));

        args.addAll(methodExpr.getArgs().stream()
                .map(e -> this.translateValue(e, varType, methodPath))
                .toList());

        return methodModel.apply(ctx, args);
    }

    public Expr translateCondition(Value condition, VarType varType, SMethodPath methodPath) {
        if (condition instanceof AbstractConditionExpr exp) {
            SAssignment holder = translateAndWrapValues(exp.getOp1(), exp.getOp2(), varType, methodPath);
            Expr e1 = holder.getLeft().getExpr();
            Expr e2 = holder.getRight().getExpr();
            if (e1.isBool() && e2.isInt()) {
                int val = ((IntNum) e2).getInt();
                e2 = ctx.mkBool(val == 1);
            } else if (e1.isInt() && e2.isBool()) {
                int val = ((IntNum) e1).getInt();
                e1 = ctx.mkBool(val == 1);
            }

            boolean e1IsNull = SortType.NULL.equals(e1.getSort());
            boolean e2IsNull = SortType.NULL.equals(e2.getSort());

            if (condition instanceof JEqExpr) {
                if (e1IsNull && e2IsNull) return ctx.mkBool(true);
                if (e1IsNull ^ e2IsNull) return ctx.mkBool(false);
                return ctx.mkEq(e1, e2);
            } else if (condition instanceof JNeExpr) {
                if (e1IsNull && e2IsNull) return ctx.mkBool(false);
                if (e1IsNull ^ e2IsNull) return ctx.mkBool(true);
                return ctx.mkNot(ctx.mkEq(e1, e2));
            } else if (condition instanceof JGtExpr) {
                if (e1IsNull || e2IsNull) return ctx.mkBool(false);
                return ctx.mkGt(e1, e2);
            } else if (condition instanceof JGeExpr) {
                if (e1IsNull || e2IsNull) return ctx.mkBool(false);
                return ctx.mkGe(e1, e2);
            } else if (condition instanceof JLtExpr) {
                if (e1IsNull || e2IsNull) return ctx.mkBool(false);
                return ctx.mkLt(e1, e2);
            } else if (condition instanceof JLeExpr) {
                if (e1IsNull || e2IsNull) return ctx.mkBool(false);
                return ctx.mkLe(e1, e2);
            }
        }
        throw new RuntimeException("Condition could not be translated: " + condition);
    }

    public SExpr wrapMethodCall(AbstractInvokeExpr invoke, Expr val) {
        MethodSignature methodSignature = invoke.getMethodSignature();

        List<Value> args = new ArrayList<>();
        Value base = null;
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            base = i.getBase();
        for (Value arg : invoke.getArgs())
            args.add(arg);
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs())
                args.add(arg);

        if (MethodModel.get(methodSignature).isPresent()) {
            return new SMethodExpr(val, invoke, base, args, MethodPropagationType.MODELLED);
        } else if (CLIOptions.shouldPropagate(methodSignature.getDeclClassType().toString())) {
            SType sType = isConstructorCall(methodSignature) ?
                    SType.INVOKE_SPECIAL_CONSTRUCTOR : SType.INVOKE;
            return new SMethodExpr(val, sType, invoke, base, args, MethodPropagationType.PROPAGATE);
        } else if (CLIOptions.shouldMock(methodSignature.getDeclClassType().toString())) {
            SType sType = isConstructorCall(methodSignature) ?
                    SType.INVOKE_MOCK_SPECIAL_CONSTRUCTOR : SType.INVOKE_MOCK;
            return new SMethodExpr(val, sType, invoke, base, args, MethodPropagationType.MOCKED);
        } else {
            return new SMethodExpr(val, SType.OTHER, invoke, base, args, MethodPropagationType.IGNORED);
        }
    }

    // if parameters are equal, make sure the method mocks the same return value
    public Expr translateMockValue(Value value,
                                   VarType varType,
                                   Method method,
                                   List<Expr> params,
                                   SMethodPath methodPath,
                                   SMethodPath topMethodPath) {
        Expr expr = translateValue(value, varType, methodPath);
        for (SMethodMockVar methodMockVar : topMethodPath.getSymbolicVarStack().getAllMocks(method)) {
            if (params.size() != methodMockVar.getArguments().size()) continue;
            BoolExpr eq = ctx.mkTrue();
            for (int i = 0; i < params.size(); i++) {
                Expr e1 = params.get(i);
                Expr e2 = methodMockVar.getArguments().get(i);
                eq = ctx.mkAnd(eq, ctx.mkEq(e1, e2));
            }
            expr = ctx.mkITE(eq, methodMockVar.getExpr(), expr);
        }
        return expr;
    }

    public Expr translateValue(Value value, VarType varType, SMethodPath methodPath) {
        return translateValue(value, value.getType(), varType, methodPath);
    }

    public Expr translateValue(Value value, Type type, VarType varType, SMethodPath methodPath) {
        if (value instanceof Local) {
            return getSymbolicValue(value, type, varType, methodPath);
        }
        if (value instanceof JFieldRef) {
            return getSymbolicValue(value, type, varType, methodPath);
        }
        else if (value instanceof JCastExpr v) {
            return getSymbolicValue(v.getOp(), type, varType, methodPath);
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
            return handleMethodCall(abstractInvoke, methodPath);
        }
        // TODO: handle arrays
        if (value instanceof JNewExpr
                || value instanceof JNewMultiArrayExpr
                || value instanceof JNewArrayExpr) {
            return ctx.mkFreshConst(value.toString(), translateType(value.getType()));
        }
        if (value instanceof JArrayRef) {
            // Create an integer sort for array indices
            IntSort intSort = ctx.getIntSort();

            // Create a sort for array elements
            Sort elementSort = translateType(value.getType());

            return ctx.mkArrayConst(value.toString(), intSort, elementSort);
        }
        if (value instanceof AbstractUnopExpr unop) {
            if (value instanceof JLengthExpr)
                return ctx.mkLength(translateValue(value, varType, methodPath));
            if (unop instanceof JNegExpr)
                return ctx.mkNot(translateValue(value, varType, methodPath));
        }
        if (value instanceof AbstractExprVisitor visitor) {
            // TODO: handle
        }
        if (value instanceof AbstractBinopExpr binop) {
            SAssignment holder = translateAndWrapValues(
                    binop.getOp1(), binop.getOp2(), varType, methodPath);
            Expr left = holder.getLeft().getExpr();
            Expr right = holder.getRight().getExpr();
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
        if (value instanceof NullConstant)
            return mkNull();
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

    private Expr handleMethodCall(AbstractInvokeExpr invoke, SMethodPath methodPath) {
        System.out.println("IN HANDLE METHOD CALL: " + invoke.getMethodSignature());

        MethodSignature methodSignature = invoke.getMethodSignature();

        List<Expr> args = new ArrayList<>();
        Expr base = null;
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            base = translateValue(i.getBase(), VarType.BASE_ARG, methodPath);
        for (Value arg : invoke.getArgs())
            args.add(translateValue(arg, VarType.METHOD_ARG, methodPath));
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs()) {
                if (!(arg instanceof MethodType) && !(arg instanceof MethodHandle))
                    args.add(translateValue(arg, VarType.METHOD_ARG, methodPath));
            }

        Optional<MethodModel> optional = MethodModel.get(methodSignature);
        if (optional.isPresent()) {
            MethodModel methodModel = optional.get();
            if (methodModel.hasBase())
                args.add(0, base);
            return methodModel.apply(ctx, args);
        } else {
            Sort sort;
            if (invoke.getMethodSignature().toString().contains(": void <init>"))
                sort = ctx.mkUninterpretedSort(invoke.getMethodSignature().getDeclClassType().toString());
            else
                sort = translateType(invoke.getType());
            return ctx.mkFreshConst(invoke.toString(), sort);
        }
    }

    private static Sort translateType(Type type) {
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
            return ctx.mkFPSort64();
        if (type instanceof PrimitiveType.DoubleType)
            return ctx.mkFPSort64();
        if (type instanceof ArrayType) {
            Sort elementSort = translateType(((ArrayType) type).getElementType());
            return ctx.mkArraySort(ctx.getIntSort(), elementSort);
        }
        if (type instanceof ReferenceType) {
            return translateReferenceType(type.toString());
        }
        if (type instanceof VoidType)
            return ctx.mkUninterpretedSort("Void");

        // For any other types, use an uninterpreted sort
        return ctx.mkUninterpretedSort(type.toString());
    }

    private static Sort translateReferenceType(String value) {
        Class<?> clazz;
        try {
            clazz = Class.forName(value);
        } catch (ClassNotFoundException e) {
            return SortType.OBJECT.value(ctx);
        }
        if (Boolean.class.isAssignableFrom(clazz))
            return ctx.mkBoolSort();
        if (Integer.class.isAssignableFrom(clazz))
            return ctx.getIntSort();
        if (Long.class.isAssignableFrom(clazz))
            return ctx.mkIntSort();
        if (Float.class.isAssignableFrom(clazz))
            return ctx.mkFPSort64();
        if (Double.class.isAssignableFrom(clazz))
            return ctx.mkFPSort64();
        if (Number.class.isAssignableFrom(clazz))
            return ctx.mkRealSort();
        if (String.class.isAssignableFrom(clazz))
            return ctx.mkStringSort();
        if (Set.class.isAssignableFrom(clazz))
            return SortType.SET.value(ctx);
//            return ctx.mkSetSort(SortType.OBJECT.value(ctx));
        if (List.class.isAssignableFrom(clazz))
            return SortType.ARRAY.value(ctx);
//            return ctx.mkListSort("ArrayList", SortType.OBJECT.value(ctx));
        if (Map.class.isAssignableFrom(clazz))
            return SortType.MAP.value(ctx);

        // TODO: handle sets & maps correctly

        return SortType.OBJECT.value(ctx);
    }

    private Expr getSymbolicValue(Value value, Type type, VarType varType, SMethodPath methodPath) {
        String key = getValueName(value);
        Optional<SVar> optional = varType == VarType.FIELD ?
                methodPath.getClassInstance().getSymbolicFieldStack().get(key)
                : methodPath.getSymbolicVarStack().get(key);
        return optional.orElseGet(() -> saveSymbolicVar(
                value, type, varType, methodPath)).getExpr();
    }

    private Expr getSymbolicValue(Value value, Sort sort, VarType varType, SMethodPath methodPath) {
        String key = getValueName(value);
        Optional<SVar> optional = varType == VarType.FIELD ?
                methodPath.getClassInstance().getSymbolicFieldStack().get(key)
                : methodPath.getSymbolicVarStack().get(key);
        return optional.orElseGet(() -> saveSymbolicVar(
                value, sort, varType, methodPath)).getExpr();
    }

    public SVar saveSymbolicVar(Value value, Type type, VarType varType, SMethodPath methodPath) {
        String name = getValueName(value);
        if (type == null) type = value.getType();
        Sort sort = translateType(type);
        Expr expr = mkExpr(name, sort);
        return updateSymbolicVar(value, expr, varType, methodPath);
    }

    public SVar saveSymbolicVar(Value value, Sort sort, VarType varType, SMethodPath methodPath) {
        String name = getValueName(value);
        Expr expr = mkExpr(name, sort);
        return updateSymbolicVar(value, expr, varType, methodPath);
    }

    public SVar updateSymbolicVar(
            Value variable, Expr expression, VarType varType, SMethodPath methodPath) {
        Class<?> classType = SootHelper.translateType(variable.getType());
        return updateSymbolicVar(variable, expression, varType, methodPath, classType);
    }

    public SVar updateSymbolicVar(
            Value variable, Expr expression, VarType varType, SMethodPath methodPath, Class<?> classType) {
        SClassInstance classInstance = methodPath.getClassInstance();
        if (expression != null) {
            String name = getValueName(variable);
            if (classInstance.getFieldNames().contains(name)) {
                varType = VarType.FIELD;
                return classInstance.getSymbolicFieldStack().add(name, classType, expression, varType);
            }
            return methodPath.getSymbolicVarStack().add(name, classType, expression, varType);
        }
        return null;
    }

    public SVar updateSymbolicVar(
            Value variable, Expr expression, VarType varType, SMethodPath methodPath, Class<?> classType, Method method, List<Expr> params) {
        String name = getValueName(variable);
        return methodPath.getSymbolicVarStack().add(name, classType, expression, varType, method, params);
    }

    public String getValueName(Value value) {
        String res = value.toString();
        if (res.startsWith("this.")) res = res.substring(5);
        return res;
    }
}
