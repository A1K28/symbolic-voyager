package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.struct.MethodModel;
import com.github.a1k28.evoc.helper.Logger;
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

import java.util.*;

public class Z3Translator {
    private static Z3ExtendedContext ctx = null;
    private static Solver solver;
    private final SMethodPath sMethodPath;
    private final SStack symbolicVarStack;
    private static final Logger log = Logger.getInstance(Z3Translator.class);

    public Z3Translator(SMethodPath sMethodPath, SStack symbolicVarStack) {
        initZ3();

        this.sMethodPath = sMethodPath;
        this.symbolicVarStack = symbolicVarStack;
    }

    public static synchronized Solver makeSolver() {
        if (solver == null) {
            solver = ctx.mkSolver();
        }
        return solver;
    }

    public static Z3ExtendedContext getContext() {
        return ctx;
    }

    public static synchronized void initZ3() {
        // Initialize Z3
        if (ctx == null) {
            ctx = new Z3ExtendedContext();
            log.info("Successfully initialized Z3 context");
        }
    }

    public static synchronized void close() {
        ctx.close();
        ctx = null;
        log.info("Successfully closed Z3 context");
    }

    public Expr mkEq(Expr expr, boolean val) {
        return mkEq(expr, ctx.mkBool(val));
    }

    public Expr mkEq(Expr expr, Expr val) {
        return ctx.mkEq(expr, val);
    }

    public Expr mkExpr(String name, Type type) {
        Sort sort = translateType(type);
        return mkExpr(name, sort);
    }

    public Expr mkExpr(String name, Sort sort) {
        return ctx.mkConst(name, sort);
    }

    public SExpr translateAndWrapValue(Value value, VarType varType) {
        if (value instanceof AbstractInvokeExpr invoke) {
            return wrapMethodCall(invoke);
        } else {
            return new SExpr(translateValue(value, varType));
        }
    }

    public SAssignment translateAndWrapValues(Value value1, Value value2, VarType varType) {
        SExpr right;
        if (value2 instanceof AbstractInvokeExpr invoke) {
            right = wrapMethodCall(invoke);
        } else {
            right = new SExpr(translateValue(value2, varType));
        }

        SExpr left;
        if (value1 instanceof Local || value1 instanceof JFieldRef) {
            Sort sort;
            if (value2.getType().getClass() == UnknownType.class)
                sort = right.getExpr().getSort();
            else
                sort = translateType(value2.getType());
            left = new SExpr(getSymbolicValue(value1, sort, varType));
        } else {
            left = new SExpr(translateValue(value1, varType));
        }


        return new SAssignment(left, right);
    }

    public Expr callProverMethod(SMethodExpr methodExpr, VarType varType) {
        MethodSignature methodSignature = methodExpr.getInvokeExpr().getMethodSignature();
        MethodModel methodModel = MethodModel.get(methodSignature).orElseThrow();

        List<Expr> args = new ArrayList<>();
        if (methodModel.hasBase() && methodExpr.getBase() != null)
            args.add(translateValue(methodExpr.getBase(), varType));

        args.addAll(methodExpr.getArgs().stream()
                .map(e -> this.translateValue(e, varType))
                .toList());

        return methodModel.apply(ctx, args);
//        if (methodModel != null) {
//        } else {
//            return handleUnknownMethod(methodExpr.getInvokeExpr(), args);
//        }
    }

    public Expr translateCondition(Value condition, VarType varType) {
        if (condition instanceof AbstractConditionExpr exp) {
            SAssignment holder = translateAndWrapValues(exp.getOp1(), exp.getOp2(), varType);
            Expr e1 = holder.getLeft().getExpr();
            Expr e2 = holder.getRight().getExpr();
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

    public SExpr wrapMethodCall(AbstractInvokeExpr invoke) {
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

//        Class.forName(invoke.getMethodSignature().getDeclClassType().toString());

        if (MethodModel.get(methodSignature).isPresent()) {
            return new SMethodExpr(invoke, base, args, false);
        } else if (!methodSignature.toString().startsWith("<" + sMethodPath.getClassname() + ":")) {
            // mock method call outside the current class
            if (invoke.getMethodSignature().getType().getClass() == VoidType.class) {
                System.out.println("ignored method " + methodSignature + " with params " + Arrays.toString(new List[]{args}));
                return new SExpr(translateValue(invoke, VarType.METHOD));
            } else {
                System.out.println("mocked method " + methodSignature + " with params " + Arrays.toString(new List[]{args}));
                return new SExpr(translateValue(invoke, VarType.METHOD_MOCK));
            }
        } else {
            return new SMethodExpr(invoke, base, args, true);
        }
    }

    public Expr translateValue(Value value, VarType varType) {
        if (value instanceof Local) {
            return getSymbolicValue(value, varType);
        }
        if (value instanceof JFieldRef) {
            return getSymbolicValue(value, varType);
        }
        else if (value instanceof JCastExpr v) {
            return getSymbolicValue(v.getOp(), varType);
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
        if (value instanceof JNewExpr ||
                value instanceof JNewMultiArrayExpr) {
            return ctx.mkConst(value.toString(), translateType(value.getType()));
        }
        // TODO: handle arrays
        if (value instanceof JNewArrayExpr) {
            return ctx.mkConst(value.toString(), translateType(value.getType()));
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
                return ctx.mkLength(translateValue(value, varType));
            if (unop instanceof JNegExpr)
                return ctx.mkNot(translateValue(value, varType));
        }
        if (value instanceof AbstractExprVisitor visitor) {
            // TODO: handle
        }
        if (value instanceof AbstractBinopExpr binop) {
            SAssignment holder = translateAndWrapValues(binop.getOp1(), binop.getOp2(), varType);
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
        System.out.println("IN HANDLE METHOD CALL: " + invoke.getMethodSignature());

        MethodSignature methodSignature = invoke.getMethodSignature();

        List<Expr> args = new ArrayList<>();
        Expr base = null;
        if (invoke instanceof AbstractInstanceInvokeExpr i)
            base = translateValue(i.getBase(), VarType.BASE_ARG);
        for (Value arg : invoke.getArgs())
            args.add(translateValue(arg, VarType.METHOD_ARG));
        if (invoke instanceof JDynamicInvokeExpr i)
            for (Value arg : i.getBootstrapArgs()) {
                if (!(arg instanceof MethodType) && !(arg instanceof MethodHandle))
                    args.add(translateValue(arg, VarType.METHOD_ARG));
                else
                    symbolicVarStack.getAll();
            }

        Optional<MethodModel> optional = MethodModel.get(methodSignature);
        if (optional.isPresent()) {
            MethodModel methodModel = optional.get();
            if (methodModel.hasBase())
                args.add(0, base);
            return methodModel.apply(ctx, args);
        } else {
            Sort sort = translateType(invoke.getType());
            return ctx.mkConst(invoke.toString(), sort);
//            return handleUnknownMethod(invoke, args);
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
            return translateReferenceType(type.toString());
        }
        if (type instanceof VoidType)
            return ctx.mkUninterpretedSort("Void");

        // For any other types, use an uninterpreted sort
        return ctx.mkUninterpretedSort(type.toString());
    }

    private Sort translateReferenceType(String value) {
        Class<?> clazz;
        try {
            clazz = Class.forName(value);
        } catch (ClassNotFoundException e) {
//            ("Class not found: " + value, e);
            return SortType.OBJECT.value(ctx);
        }
        if (Boolean.class.isAssignableFrom(clazz))
            return ctx.mkBoolSort();
        if (Integer.class.isAssignableFrom(clazz))
            return ctx.getIntSort();
        if (Long.class.isAssignableFrom(clazz))
            return ctx.mkIntSort();
        if (Float.class.isAssignableFrom(clazz))
            return ctx.mkFPSort32();
        if (Double.class.isAssignableFrom(clazz))
            return ctx.mkFPSort64();
        if (Number.class.isAssignableFrom(clazz))
            return ctx.mkRealSort();
        if (String.class.isAssignableFrom(clazz))
            return ctx.mkStringSort();
        if (Set.class.isAssignableFrom(clazz))
            return ctx.mkSetSort(SortType.OBJECT.value(ctx));
        if (List.class.isAssignableFrom(clazz))
            return ctx.mkListSort("ArrayList", SortType.OBJECT.value(ctx));

        // TODO: handle sets & maps correctly

        return SortType.OBJECT.value(ctx);
    }

    private Expr getSymbolicValue(Value value, VarType varType) {
        return getSymbolicVar(value, null, varType).getExpr();
    }

    private Expr getSymbolicValue(Value value, Sort sort, VarType varType) {
        return getSymbolicVarBySort(value, sort, varType).getExpr();
    }

    public SVar saveSymbolicVar(Value value, Type type, VarType varType) {
        String name = getValueName(value);
        if (type == null) type = value.getType();
        Expr expr = mkExpr(name, type);
        return updateSymbolicVariable(value, expr, varType);
    }

    public SVar saveSymbolicVar(Value value, Sort sort, VarType varType) {
        String name = getValueName(value);
        Expr expr = mkExpr(name, sort);
        return updateSymbolicVariable(value, expr, varType);
    }

    public String getValueName(Value value) {
        String res = value.toString();
        if (res.startsWith("this.")) res = res.substring(5);
        return res;
    }

    public SVar updateSymbolicVariable(Value variable, Expr expression, VarType varType) {
        if (variable != null && expression != null) {
            String name = getValueName(variable);
            if (sMethodPath.getFields().contains(name)) varType = VarType.FIELD;
            return symbolicVarStack.add(name, variable, expression, varType);
        }
        return null;
    }

    private SVar getSymbolicVar(Value value, Type type, VarType varType) {
        String key = getValueName(value);
        Optional<SVar> optional = symbolicVarStack.get(key);
        return optional.orElseGet(() -> saveSymbolicVar(value, type, varType));
    }

    private SVar getSymbolicVarBySort(Value value, Sort sort, VarType varType) {
        String key = getValueName(value);
        Optional<SVar> optional = symbolicVarStack.get(key);
        return optional.orElseGet(() -> saveSymbolicVar(value, sort, varType));
    }
}
