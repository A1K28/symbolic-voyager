package com.github.a1k28.symvoyager.core.symbolicexecutor;

import com.github.a1k28.symvoyager.helper.Logger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import sootup.core.graph.BasicBlock;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.model.Position;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.*;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SootInterpreter {
    private static final Logger log = Logger.getInstance(SootInterpreter.class);
    private static final Map<String, Class<?>> cachedMap = new HashMap<>();

    public static JavaSootClass getSootClass(String className) {
        Optional<JavaSootClass> optional = getJavaSootClassSource(className);
        for (int i = 0; i < 3 && optional.isEmpty(); i++) {
            // the class may have not been loaded properly. wait for a bit.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            optional = getJavaSootClassSource(className);
        }

        return optional.get();
    }

    private static Optional<JavaSootClass> getJavaSootClassSource(
            String className) {
        AnalysisInputLocation inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));
        JavaView view = new JavaView(Collections.singletonList(inputLocation));
        JavaIdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();
        ClassType classType = identifierFactory.getClassType(className);
//        initArrayMap(identifierFactory);
        return view.getClass(classType);
    }

    public static SootMethod getSootMethod(
            SootClass sootClass, Executable method, boolean isConstructor) {
        // Get the method
        outer: for (SootMethod sootMethod : sootClass.getMethods()) {
            if (!isConstructor && !method.getName().equals(sootMethod.getName())) continue;
            List<Type> sootTypes = sootMethod.getParameterTypes();
            Class<?>[] types = method.getParameterTypes();
            if (types.length != sootTypes.size()) continue;
            for (int i = 0; i < types.length; i++)
                if (!types[i].getTypeName().equals(sootTypes.get(i).toString()))
                    continue outer;
            return sootMethod;
        }
        throw new IllegalStateException("Could not match method: " + method);
    }

    public static Executable getMethod(AbstractInvokeExpr invokeExpr) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(invokeExpr.getMethodSignature().getDeclClassType().toString());
        boolean isConstructorCall = isConstructorCall(invokeExpr.getMethodSignature());
        Executable[] executables = isConstructorCall ?
                clazz.getDeclaredConstructors() : clazz.getDeclaredMethods();
        List<Type> sootParamTypes = invokeExpr.getMethodSignature().getParameterTypes();
        String methodName = invokeExpr.getMethodSignature().getName();
        outer: for (Executable method : executables) {
            if (!isConstructorCall && !method.getName().equals(methodName)) continue;
            Class<?>[] methodParamTypes = method.getParameterTypes();
            if (methodParamTypes.length != sootParamTypes.size()) continue;
            for (int i = 0; i < methodParamTypes.length; i++)
                if (!methodParamTypes[i].getTypeName().equals(sootParamTypes.get(i).toString()))
                    continue outer;
            return method;
        }
        throw new IllegalStateException("Could not match method: " + invokeExpr.getMethodSignature());
    }

    public static Class<?> getClass(ClassType classType) {
        return getClass(classType.getFullyQualifiedName());
    }

    public static Class<?> getClass(String classname) {
        try {
            if (!cachedMap.containsKey(classname)) {
                Class clazz = Class.forName(classname);
                cachedMap.put(classname, clazz);
            }
            return cachedMap.get(classname);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<JavaSootField> getFields(SootClass sootClass) throws ClassNotFoundException {
        List<JavaSootField> fields = new ArrayList<>();
        addFields(sootClass, fields);
        return fields;
    }

    public static Class translateType(Type type) {
        try {
            if (type.getClass() == PrimitiveType.BooleanType.class)
                return boolean.class;
            if (type.getClass() == PrimitiveType.ByteType.class)
                return byte.class;
            if (type.getClass() == PrimitiveType.ShortType.class)
                return short.class;
            if (type.getClass() == PrimitiveType.CharType.class)
                return char.class;
            if (type.getClass() == PrimitiveType.IntType.class)
                return int.class;
            if (type.getClass() == PrimitiveType.LongType.class)
                return long.class;
            if (type.getClass() == PrimitiveType.FloatType.class)
                return float.class;
            if (type.getClass() == PrimitiveType.DoubleType.class)
                return double.class;
            if (type.getClass() == ArrayType.class)
                return translateType(((ArrayType) type).getElementType()).arrayType();
            if (type.getClass() == UnknownType.class)
                return Object.class;
            if (type instanceof JavaClassType classType && type.toString().endsWith("[]")) {
                JavaClassType javaClassType = new JavaClassType(
                        classType.getClassName().substring(0, classType.getClassName().length()-2),
                        classType.getPackageName());
                return translateType(javaClassType).arrayType();
            }
            return Class.forName(type.toString());
        } catch (Exception e) {
            // TODO: possibly modify behavior
            log.warn("Could not find class for type: " + type);
            return Object.class;
        }
    }

    public static boolean isConstructorCall(MethodSignature methodSignature) {
        return methodSignature.toString().matches(".*<.*: void <init>\\(.*\\)>.*");
    }

    private static void addFields(SootClass sootClass, List<JavaSootField> fields) throws ClassNotFoundException {
        sootClass.getFields().forEach(e -> {if (e instanceof JavaSootField j) fields.add(j);});
//        if (sootClass.hasSuperclass()) {
//            // TODO: stop within the same package?
//            String name = sootClass.getSuperclass().get().toString();
//            if (!name.equals(Object.class.getName())) {
//                SootClass<?> parent = SootInterpreter.getSootClass(sootClass.getSuperclass().get().toString());
//                addFields(parent, fields);
//            }
//        }
    }

    public static String translateField(Field field) {
        return new StringBuilder("<")
                .append(field.getDeclaringClass().getName())
                .append(": ")
                .append(field.getType().getName())
                .append(" ")
                .append(field.getName())
                .append(">")
                .toString();
    }

    public static int compareBlocks(BasicBlock<?> block1, BasicBlock<?> block2) {
        int pos1 = findPosition(block1, false);
        int pos2 = findPosition(block2, false);
        return Integer.compare(pos1, pos2);
    }

    private static int findPosition(BasicBlock<?> block, boolean reversed) {
        if (reversed) {
            for (int i = block.getStmts().size()-1; i >= 0; i--) {
                Position position = block.getStmts().get(i).getPositionInfo().getStmtPosition();
                if (position instanceof NoPositionInformation) continue;
                return position.getFirstLine();
            }
        } else {
            for (int i = 0; i < block.getStmts().size(); i++) {
                Position position = block.getStmts().get(i).getPositionInfo().getStmtPosition();
                if (position instanceof NoPositionInformation) continue;
                return position.getFirstLine();
            }
        }
        if (block.getPredecessors().size() != 1) return 0;
        return findPosition(block.getPredecessors().get(0), true);
    }
}
