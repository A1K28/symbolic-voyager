package com.github.a1k28.evoc.helper;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RuntimeCompiler {
    private static final Logger log = Logger.getInstance(RuntimeCompiler.class);

    public static String compile(String code, String targetClasspath) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(code);
        String packageName =  extractPackageName(cu);
        String className = extractClassName(cu);

        // Save source in .java file.
        File root = Files.createTempDirectory("java").toFile();
        File sourceFile = new File(root, className+".java");
        sourceFile.getParentFile().mkdirs();
        Files.writeString(sourceFile.toPath(), code);

        log.info("Created: " + className+".java file under: " + root.getPath());

        String classpath = System.getProperty("java.class.path");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        String[] javacOpts = {
                "-classpath",
                classpath + File.pathSeparator + targetClasspath,
                sourceFile.getPath()};

        try(OutputStream err = new ByteArrayOutputStream()) {
            int status = javac.run(null, null, err,  javacOpts);
            if (status != 0) {
                String error = err.toString().replace(sourceFile.getPath(), "");
                log.error("Invalid javac status: " + status + ". " + error);
                throw new RuntimeException(error);
            }
        }

        String classFilePath = sourceFile.getParentFile() + File.separator + className + ".class";
        log.info("Successfully compiled " + classFilePath);

        String targetPath = targetClasspath;
        if (packageName != null) {
            targetPath += File.separator + packageName.replace(".", File.separator);
            Files.createDirectories(Paths.get(targetPath));
            log.info("Created directory: " + targetPath);
        }

        String classFileNewPath = targetPath + File.separator + className + ".class";
        File classFile = new File(classFilePath);
        Files.move(classFile.toPath(), Paths.get(classFileNewPath), StandardCopyOption.REPLACE_EXISTING);
        log.info("Moved file from: " + classFilePath + " to: " + classFileNewPath);

        if (sourceFile.delete()) {
            if (root.delete()) {
                log.info("Removed temp directory: " + root.getPath());
            }
        }

        if (packageName != null && !packageName.isEmpty()) className = packageName + "." + className;

        return className;
    }

    private static String extractPackageName(CompilationUnit cu) {
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration().orElse(null);
        if (packageDeclaration == null) return null;
        return packageDeclaration.getName().asString();
    }

    private static String extractClassName(CompilationUnit cu) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in file: " + cu))
                .getNameAsString();
    }

    public static void main(String[] args) throws Exception {
        compile("""
                package com.asdf;
                
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                import java.util.Stack;
                               
                import java.util.EmptyStackException;
                               
                import static org.junit.jupiter.api.Assertions.*;
                               
                class StackTest {
                    private Stack<String> stack;
                               
                    @BeforeEach
                    void setUp() {
                        stack = new Stack<>();
                    }
                               
                    @Test
                    void testPushAndPop() {
                        stack.push("Hello");
                        stack.push("World");
                        assertEquals("World", stack.pop());
                        assertEquals("Hello", stack.pop());
                        assertTrue(stack.isEmpty());
                    }
                               
                    @Test
                    void testPushExceedingCapacity() {
                        for (int i = 0; i < 10; i++) {
                            stack.push("Element " + i);
                        }
                        assertThrows(RuntimeException.class, () -> stack.push("One more"));
                    }
                               
                    @Test
                    void testPopEmptyStack() {
                        assertThrows(EmptyStackException.class, () -> stack.pop());
                    }
                               
                    @Test
                    void testIsEmpty() {
                        assertTrue(stack.isEmpty());
                        stack.push("Element");
                        assertFalse(stack.isEmpty());
                        stack.pop();
                        assertTrue(stack.isEmpty());
                    }
                               
                    @Test
                    void testPushNullObject() {
                        stack.push(null);
                        assertNull(stack.pop());
                    }
                               
                    @Test
                    void testPushAndPopMultipleElements() {
                        for (int i = 0; i < 20; i++) {
                            stack.push("Element " + i);
                        }
                               
                        for (int i = 19; i >= 10; i--) {
                            assertEquals("Element " + i, stack.pop());
                        }
                               
                        assertThrows(RuntimeException.class, () -> stack.push("One more"));
                    }
                               
                    @Test
                    void testPopAllElements() {
                        for (int i = 0; i < 10; i++) {
                            stack.push("Element " + i);
                        }
                               
                        for (int i = 9; i >= 0; i--) {
                            assertEquals("Element " + i, stack.pop());
                        }
                               
                        assertTrue(stack.isEmpty());
                        assertThrows(EmptyStackException.class, () -> stack.pop());
                    }
                               
                    @Test
                    void testIsEmptyWithNullElements() {
                        stack.push(null);
                        assertFalse(stack.isEmpty());
                        stack.pop();
                        assertTrue(stack.isEmpty());
                    }
                }
               """, "/Users/ak/Desktop/IdeaProjects/test/target/classes");
    }
}
