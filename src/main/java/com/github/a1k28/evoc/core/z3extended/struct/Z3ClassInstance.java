package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SClassInstance;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.evoc.core.z3extended.Z3CachingFactory;
import com.github.a1k28.evoc.core.z3extended.Z3ExtendedSolver;
import com.github.a1k28.evoc.core.z3extended.model.ClassInstanceModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.helper.SootHelper;
import com.github.a1k28.evoc.model.common.IStack;
import com.microsoft.z3.Context;
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

import static com.github.a1k28.evoc.core.z3extended.Z3Translator.mkNull;
import static com.github.a1k28.evoc.helper.SootHelper.*;

public class Z3ClassInstance implements IStack {
    private final Context ctx;
    private final Z3Stack<Integer, ClassInstanceModel> stack;

    public Z3ClassInstance(Context ctx) {
        this.ctx = ctx;
        this.stack = new Z3Stack<>();
    }

    @Override
    public void push() {
        stack.push();
    }

    @Override
    public void pop() {
        stack.pop();
    }

    public ClassInstanceModel constructor(Class<?> clazz) throws ClassNotFoundException {
        assert clazz != null;
        int clazzHC = ihc(clazz);
        Optional<ClassInstanceModel> optional = stack.get(clazzHC);
        if (optional.isEmpty()) {
            Expr expr = ctx.mkConst("Object"+clazzHC, SortType.OBJECT.value(ctx));
            SClassInstance sClassInstance = createClassInstance(clazz);
            ClassInstanceModel classInstanceModel = new ClassInstanceModel(
                    clazzHC, expr, null, sClassInstance);
            stack.add(clazzHC, classInstanceModel);
        }
        return stack.get(clazzHC).orElseThrow();
    }

    public ClassInstanceModel constructor(Expr base, Class<?> clazz) throws ClassNotFoundException {
        assert base != null && clazz != null;
        int baseHC = ihc(base);
        Optional<ClassInstanceModel> optional = stack.get(baseHC);
        if (optional.isEmpty()) {
            Expr expr = ctx.mkConst("Object"+baseHC, SortType.OBJECT.value(ctx));
            SClassInstance sClassInstance = createClassInstance(clazz);
            ClassInstanceModel classInstanceModel = new ClassInstanceModel(
                    baseHC, expr, base, sClassInstance);
            stack.add(baseHC, classInstanceModel);
        }
        return stack.get(baseHC).orElseThrow();
    }

    public Expr initialize(Expr expr) {
        int hashCode = ihc(expr);
        ClassInstanceModel model = stack.get(hashCode).orElseThrow();
        SClassInstance instance = model.getClassInstance();

        for (JavaSootField field : instance.getFields()) {
            String name = field.toString();
            Expr defaultValue = getDefaultValue(field.getType());
            Class<?> classType = SootHelper.translateType(field.getType());
            instance.getSymbolicFieldStack().add(name, classType, defaultValue, VarType.FIELD);
        }

        return model.getExpr();
    }

    public Optional<ClassInstanceModel> getClassInstance(Expr expr) {
        int hashCode = ihc(expr);
        return stack.get(hashCode);
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
        List<JavaSootField> fields = SootHelper.getFields(sootClass);
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
        return mkNull();
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
