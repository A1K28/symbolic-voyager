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

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.github.a1k28.evoc.core.z3extended.Z3Translator.mkNull;
import static com.github.a1k28.evoc.helper.SootHelper.*;

public class Z3ClassInstance implements IStack {
    private final Context ctx;
    private final Z3ExtendedSolver solver;
    private final Z3CachingFactory sortState;
    private final Z3Stack<Integer, ClassInstanceModel> stack;

    public Z3ClassInstance(Context ctx,
                           Z3CachingFactory sortState,
                           Z3ExtendedSolver solver) {
        this.ctx = ctx;
        this.solver = solver;
        this.sortState = sortState;
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

    private SClassInstance createClassInstance(Class<?> clazz)
            throws ClassNotFoundException {
        SootClass<JavaSootClassSource> sootClass = getSootClass(clazz.getName());
        List<JavaSootField> fields = SootHelper.getFields(sootClass);
        SClassInstance instance = new SClassInstance(clazz, fields, null);

        for (Method method : clazz.getDeclaredMethods()) {
            SMethodPath sMethodPath = createMethodPath(sootClass, instance, method, false);
            instance.getMethodPathSkeletons().put(method, sMethodPath);
        }

        for (Executable method : clazz.getDeclaredConstructors()) {
            SMethodPath sMethodPath = createMethodPath(sootClass, instance, method, true);
            instance.getMethodPathSkeletons().put(method, sMethodPath);
        }

        return instance;
    }

    private SMethodPath createMethodPath(
            SootClass<JavaSootClassSource> sootClass,
            SClassInstance classInstance,
            Executable method,
            boolean isConstructor) {
        // Find all paths
        SootMethod sootMethod = getSootMethod(sootClass, method, isConstructor);
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
        if (type.getClass() == PrimitiveType.BooleanType.class)
            return ctx.mkBool(false);
        if (type.getClass() == PrimitiveType.ByteType.class)
            return ctx.mkInt(0);
        if (type.getClass() == PrimitiveType.ShortType.class)
            return ctx.mkInt(0);
        if (type.getClass() == PrimitiveType.CharType.class)
            return ctx.mkConst("\u0000", ctx.mkCharSort());
        if (type.getClass() == PrimitiveType.IntType.class)
            return ctx.mkInt(0);
        if (type.getClass() == PrimitiveType.LongType.class)
            return ctx.mkInt(0);
        if (type.getClass() == PrimitiveType.FloatType.class)
            return ctx.mkInt(0);
        if (type.getClass() == PrimitiveType.DoubleType.class)
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
