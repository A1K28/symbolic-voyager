package com.github.a1k28.evoc.helper;

import com.github.a1k28.evoc.core.symbex.struct.SNode;
import com.github.a1k28.evoc.core.symbex.struct.SPath;
import com.github.a1k28.evoc.core.symbex.struct.SType;
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
import sootup.java.core.language.JavaLanguage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@NoArgsConstructor
public class SootHelper {
    public static SPath createFlowDiagram(String className, String methodName)
            throws ClassNotFoundException {
        SootMethod method = getSootMethod(className, methodName);
        Body body = method.getBody();

        // Generate CFG
        StmtGraph<?> cfg = body.getStmtGraph();
        return createFlowDiagram(cfg);
    }

    private static SPath createFlowDiagram(StmtGraph<?> cfg) {
        SPath sPath = new SPath();
        Stmt start = cfg.getStartingStmt();
        dfs(cfg, start, sPath, sPath.getRoot());
        return sPath;
    }

    private static void dfs(StmtGraph<?> cfg, Stmt current, SPath sPath, SNode parent) {
        SNode node = sPath.createNode(current);
        parent.addChild(node);

        if (!cfg.getTails().contains(current)) {
            List<Stmt> succs = cfg.getAllSuccessors(current);
            if (node.getType() == SType.BRANCH) {
                if (succs.size() != 2) throw new RuntimeException("Invalid branch successor size");
                SNode node2 = sPath.createNode(current);
                parent.addChild(node2);

                node.setType(SType.BRANCH_FALSE);
                node2.setType(SType.BRANCH_TRUE);

                dfs(cfg, succs.get(0), sPath, node);
                dfs(cfg, succs.get(1), sPath, node2);
            } else {
                for (Stmt succ : succs) {
                    if (!node.containsParent(succ)) {
                        dfs(cfg, succ, sPath, node);
                    }
                }
            }
        }
    }

    private static SootMethod getSootMethod(String className, String methodName) throws ClassNotFoundException {
        int javaVersion = getJavaVersion(Class.forName(className));

        AnalysisInputLocation<JavaSootClass> inputLocation =
                new JavaClassPathAnalysisInputLocation(System.getProperty("java.class.path"));

        JavaLanguage language = new JavaLanguage(javaVersion);

        Project project = JavaProject.builder(language)
                .addInputLocation(inputLocation).build();

        ClassType classType =
                project.getIdentifierFactory().getClassType(className);

        View<?> view = project.createView();

        SootClass<JavaSootClassSource> sootClass =
                (SootClass<JavaSootClassSource>) view.getClass(classType).get();

        // Get the method
        return sootClass.getMethods().stream()
                .filter(e -> e.getName().equals(methodName)).findFirst().get();
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
