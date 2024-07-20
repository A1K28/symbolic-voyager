package com.github.a1k28.evoc.helper;

import com.github.a1k28.evoc.core.symbolicexecutor.struct.SNode;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import sootup.core.Project;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootClassSource;
import sootup.java.core.JavaSootField;
import sootup.java.core.language.JavaLanguage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SootHelper {
    public static SootClass<JavaSootClassSource> getSootClass(String className) throws ClassNotFoundException {
        int javaVersion = getJavaVersion(Class.forName(className));

        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        JavaLanguage language = new JavaLanguage(javaVersion);

        Project project = JavaProject.builder(language)
                .addInputLocation(inputLocation).build();

        ClassType classType =
                project.getIdentifierFactory().getClassType(className);

        View<?> view = project.createView();

        return (SootClass<JavaSootClassSource>) view.getClass(classType).get();
    }

    public static SootMethod getSootMethod(SootClass<JavaSootClassSource> sootClass, String methodName) {
        // Get the method
        return sootClass.getMethods().stream()
                .filter(e -> e.getSignature().getSubSignature().toString().equals(methodName)
                        || e.getName().equals(methodName)).findFirst().get();
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

    public static void createFlowDiagram(SMethodPath sMethodPath, Body body) {
        StmtGraph<?> cfg = body.getStmtGraph();
        Stmt start = cfg.getStartingStmt();
        dfs(cfg, start, sMethodPath, sMethodPath.getRoot());
    }

    public static List<JavaSootField> getFields(SootClass<?> sootClass) throws ClassNotFoundException {
        List<JavaSootField> fields = new ArrayList<>();
        addFields(sootClass, fields);
        return fields;
    }

    private static void addFields(SootClass<?> sootClass, List<JavaSootField> fields) throws ClassNotFoundException {
        sootClass.getFields().forEach(e -> {if (e instanceof JavaSootField j) fields.add(j);});
        if (sootClass.hasSuperclass()) {
            // TODO: stop within the same package?
            String name = sootClass.getSuperclass().get().toString();
            if (!name.equals(Object.class.getName())) {
                SootClass<?> parent = SootHelper.getSootClass(sootClass.getSuperclass().get().toString());
                addFields(parent, fields);
            }
        }
    }

    private static void dfs(StmtGraph<?> cfg, Stmt current, SMethodPath sMethodPath, SNode parent) {
        SNode node = sMethodPath.createNode(current);
        parent.addChild(node);

        sMethodPath.incrementTotalLines();
//        System.out.println(node);

        if (!cfg.getTails().contains(current)) {
            List<Stmt> succs = cfg.getAllSuccessors(current);
            if (node.getType() == SType.BRANCH) {
                if (succs.size() != 2) throw new RuntimeException("Invalid branch successor size");
                SNode node2 = sMethodPath.createNode(current);
                parent.addChild(node2);

                node.setType(SType.BRANCH_FALSE);
                node2.setType(SType.BRANCH_TRUE);

                dfs(cfg, succs.get(0), sMethodPath, node);
                dfs(cfg, succs.get(1), sMethodPath, node2);
            } else {
                for (Stmt succ : succs) {
                    if (!node.containsParent(succ)) {
                        dfs(cfg, succ, sMethodPath, node);
                    }
                }
            }
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
}
