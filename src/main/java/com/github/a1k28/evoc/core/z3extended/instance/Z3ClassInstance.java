package com.github.a1k28.evoc.core.z3extended.instance;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SClassInstance;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedContext;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.ClassInstanceModel;
import com.github.a1k28.evoc.core.z3extended.model.IStack;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.struct.Z3SortUnion;
import com.github.a1k28.evoc.core.z3extended.struct.Z3Stack;
import com.github.a1k28.evoc.core.sootup.SootInterpreter;
import com.microsoft.z3.Expr;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.github.a1k28.evoc.core.sootup.SootInterpreter.*;

public class Z3ClassInstance extends Z3AbstractHybridInstance implements IStack {
    private final Z3Stack<String, ClassInstanceModel> stack;
    private final Z3SortUnion sortUnion;

    public Z3ClassInstance(Z3ExtendedContext ctx,
                           Z3ExtendedSolver solver,
                           Z3SortUnion sortUnion) {
        super(ctx, solver);
        this.stack = new Z3Stack<>();
        this.sortUnion = sortUnion;

        defineMapFunc("ClassArrayReferenceMap", sortUnion.getGenericSort());
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public ClassInstanceModel parameterConstructor(Expr base, Class<?> clazz) {
        try {
            return constructor(base, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ClassInstanceModel constructor(Class<?> clazz) throws ClassNotFoundException {
        assert clazz != null;
        String clazzHC = String.valueOf(System.identityHashCode(clazz));
        Optional<ClassInstanceModel> optional = stack.get(clazzHC);
        if (optional.isEmpty()) {
            Expr expr = ctx.mkConst("Object"+clazzHC, SortType.OBJECT.value(ctx));
            SClassInstance sClassInstance = createClassInstance(clazz);
            ClassInstanceModel classInstanceModel = new ClassInstanceModel(
                    expr, null, sClassInstance);
            stack.add(clazzHC, classInstanceModel);
        }
        return stack.get(clazzHC).orElseThrow();
    }

    public ClassInstanceModel constructor(Expr base, Class<?> clazz) throws ClassNotFoundException {
        assert base != null && clazz != null;
        Expr wrappedBase = sortUnion.wrapValue(base);
        Optional<String> optional = evalReferenceStrict(wrappedBase);
        String ref;
        if (optional.isEmpty()) {
            createMapping(wrappedBase);
            ref = evalReference(wrappedBase);
            Expr expr = ctx.mkConst("Object"+ref, SortType.OBJECT.value(ctx));
            SClassInstance sClassInstance = createClassInstance(clazz);
            ClassInstanceModel classInstanceModel = new ClassInstanceModel(
                    expr, base, sClassInstance);
            stack.add(ref, classInstanceModel);
        } else {
            ref = optional.get();
        }
        return stack.get(ref).orElseThrow();
    }

    public Expr initialize(Expr expr, Class<?> clazz) {
        Expr wrappedExpr = sortUnion.wrapValue(expr);
        Optional<String> ref = evalReferenceStrict(wrappedExpr);

        ClassInstanceModel model;
        if (ref.isEmpty()) {
            model = parameterConstructor(expr, clazz);
        } else {
            model = stack.get(ref.get()).orElseThrow();
        }

        SClassInstance instance = model.getClassInstance();

        for (JavaSootField field : instance.getFields()) {
            String name = field.toString();
            Expr defaultValue = getDefaultValue(field.getType());
            Class<?> classType = SootInterpreter.translateType(field.getType());
            instance.getSymbolicFieldStack().add(name, classType, defaultValue, VarType.FIELD);
        }

        return model.getExpr();
    }

    public Optional<ClassInstanceModel> getInstance(Expr expr) {
        Expr wrappedExpr = sortUnion.wrapValue(expr);
        String ref = evalReference(wrappedExpr);
        return stack.get(ref);
    }

    // lazily initialize methods & constructors
    public SMethodPath getMethodPath(SClassInstance instance, Executable method) {
        if (instance.getMethodPathSkeletons().containsKey(method))
            return instance.getMethodPathSkeletons().get(method);
        boolean isConstructor = method instanceof Constructor;
        SMethodPath sMethodPath = createMethodPath(instance, method, isConstructor);
        instance.getMethodPathSkeletons().put(method, sMethodPath);
        return sMethodPath;
    }

    private SClassInstance createClassInstance(Class<?> clazz)
            throws ClassNotFoundException {
        SootClass<JavaSootClassSource> sootClass = getSootClass(clazz.getName());
        List<JavaSootField> fields = SootInterpreter.getFields(sootClass);
        SClassInstance instance = new SClassInstance(clazz, sootClass, fields);
        return instance;
    }

    private SMethodPath createMethodPath(
            SClassInstance classInstance,
            Executable method,
            boolean isConstructor) {
        // Find all paths
        SootMethod sootMethod = getSootMethod(classInstance.getSootClass(), method, isConstructor);
        Body body = sootMethod.getBody();
        SMethodPath sMethodPath;
        if (isConstructor)
            sMethodPath = new SMethodPath(classInstance, body, null);
        else
            sMethodPath = new SMethodPath(classInstance, body, (Method) method);
        createFlowDiagram(sMethodPath, body);
        return sMethodPath;
    }

    public Expr getDefaultValue(Type type) {
        Class<?> clazz = type.getClass();
        if (clazz == PrimitiveType.BooleanType.class)
            return ctx.mkBool(false);
        if (clazz == PrimitiveType.ByteType.class)
            return ctx.mkInt(0);
        if (clazz == PrimitiveType.ShortType.class)
            return ctx.mkInt(0);
        if (clazz == PrimitiveType.CharType.class)
            return ctx.mkConst("\u0000", ctx.mkCharSort());
        if (clazz == PrimitiveType.IntType.class)
            return ctx.mkInt(0);
        if (clazz == PrimitiveType.LongType.class)
            return ctx.mkInt(0);
        if (clazz == PrimitiveType.FloatType.class)
            return ctx.mkInt(0);
        if (clazz == PrimitiveType.DoubleType.class)
            return ctx.mkInt(0);
        return ctx.mkNull();
    }

    private static int ihc(Object o) {
        if (o instanceof Expr) {
            String val = o.toString().replace("\"","");
            if (val.startsWith("Object")) {
                try {
                    val = val.replace("Object", "");
                    int lastIdx = val.indexOf('!');
                    lastIdx = lastIdx == -1 ? val.length() : lastIdx;
                    val = val.substring(0, lastIdx);
                    return Integer.parseInt(val);
                } catch (NumberFormatException ignored) {}
            }
        }
        return System.identityHashCode(o);
    }
}
