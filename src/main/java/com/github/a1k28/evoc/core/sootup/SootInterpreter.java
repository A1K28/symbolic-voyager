package com.github.a1k28.evoc.core.sootup;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import sootup.core.Project;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.*;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;
import sootup.java.core.language.JavaLanguage;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.*;

import static com.github.a1k28.evoc.core.sootup.SootParser.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SootInterpreter {
    private static final Map<String, Class<?>> cachedMap = new HashMap<>();

    public static SootClass<JavaSootClassSource> getSootClass(String className) throws ClassNotFoundException {
        int javaVersion = getJavaVersion(Class.forName(className));
        JavaLanguage language = new JavaLanguage(javaVersion);

        Optional<SootClass<JavaSootClassSource>> optional = getJavaSootClassSource(
                className, language);
        for (int i = 0; i < 3 && optional.isEmpty(); i++) {
            // the class may have not been loaded properly. wait for a bit.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            optional = getJavaSootClassSource(className, language);
        }

        return optional.get();
    }

    private static Optional<SootClass<JavaSootClassSource>> getJavaSootClassSource(
            String className, JavaLanguage language) {
        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        Project project = JavaProject.builder(language)
                .addInputLocation(inputLocation).build();

        ClassType classType =
                project.getIdentifierFactory().getClassType(className);

        View<?> view = project.createView();
        initArrayMap(view);

        return (Optional<SootClass<JavaSootClassSource>>) view.getClass(classType);
    }

    public static SootMethod getSootMethod(
            SootClass<JavaSootClassSource> sootClass, Executable method, boolean isConstructor) {
        // Get the method
        outer: for (SootMethod sootMethod : sootClass.getMethods()) {
            if (!isConstructor && !method.getName().equals(sootMethod.getName())) continue;
            List<Type> sootTypes = sootMethod.getParameterTypes();
            Class<?>[] types = method.getParameterTypes();
            if (types.length != sootTypes.size()) continue;
            for (int i = 0; i < types.length; i++)
                if (!types[i].getName().equals(sootTypes.get(i).toString()))
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
                if (!methodParamTypes[i].getName().equals(sootParamTypes.get(i).toString()))
                    continue outer;
            return method;
        }
        throw new IllegalStateException("Could not match method: " + invokeExpr.getMethodSignature());
    }

    public static int getJavaVersion(Class<?> clazz) {
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(
                clazz.getName().replace(".", File.separator) + ".class");
             DataInputStream dis = new DataInputStream(is)) {

            dis.readInt(); // Skip magic number
            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            return mapVersionToJava(majorVersion, minorVersion);
        } catch (IOException e) {
            throw new RuntimeException("Error reading class file", e);
        }
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

    public static void createFlowDiagram(SMethodPath sMethodPath, Body body) {
        StmtGraph<?> cfg = body.getStmtGraph();
//        Stmt start = cfg.getStartingStmt();
        BasicBlock<?> block = cfg.getStartingStmtBlock();
//        print(cfg, start, 0);
//        dfs(cfg, start, sMethodPath, sMethodPath.getRoot());
        interpretSoot(block, sMethodPath);
    }

    public static List<JavaSootField> getFields(SootClass<?> sootClass) throws ClassNotFoundException {
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
            return Class.forName(type.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isConstructorCall(MethodSignature methodSignature) {
        return methodSignature.toString().matches(".*<.*: void <init>\\(.*\\)>.*");
    }

    private static void addFields(SootClass<?> sootClass, List<JavaSootField> fields) throws ClassNotFoundException {
        sootClass.getFields().forEach(e -> {if (e instanceof JavaSootField j) fields.add(j);});
        if (sootClass.hasSuperclass()) {
            // TODO: stop within the same package?
            String name = sootClass.getSuperclass().get().toString();
            if (!name.equals(Object.class.getName())) {
                SootClass<?> parent = SootInterpreter.getSootClass(sootClass.getSuperclass().get().toString());
                addFields(parent, fields);
            }
        }
    }

    private static void print(StmtGraph<?> cfg, Stmt current, int level) {
        for (int i = 1; i < level; i++) System.out.print("\t");
        System.out.println(current);
        List<Stmt> succs = cfg.getAllSuccessors(current);
        succs.forEach(e -> print(cfg, e, level+1));
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
}