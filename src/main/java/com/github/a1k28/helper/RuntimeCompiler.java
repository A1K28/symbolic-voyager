package com.github.a1k28.helper;

import lombok.NoArgsConstructor;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class RuntimeCompiler {
    private static final Logger log = Logger.getInstance(RuntimeCompiler.class);
    private static final Pattern pattern = Pattern.compile("class [a-zA-Z]* \\{");

    public static String compile(String code) throws Exception {
        Matcher m = pattern.matcher(code);
        if (!m.find()) {
            throw new RuntimeException("Could not locate class name in code:\n" + code);
        }
        String className = m.group(0).split(" ")[1];

        // Save source in .java file.
        File root = Files.createTempDirectory("java").toFile();
        File sourceFile = new File(root, className+".java");
        sourceFile.getParentFile().mkdirs();
        Files.writeString(sourceFile.toPath(), code);

        log.info("Created: " + className+".java file");

        String classpath = System.getProperty("java.class.path");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        String[] javacOpts = {
                "-classpath",
                classpath,
                sourceFile.getPath()};

        int status = javac.run(null, null, null,  javacOpts);
        if (status != 0) {
            throw new RuntimeException("Invalid javac status: " + status);
        }

        String classFile = sourceFile.getParentFile() + File.separator + className + ".class";
        log.info("Successfully compiled " + classFile);
        return classFile;
    }

    public static void main(String[] args) throws Exception {
        compile("""
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                import com.github.a1k28.Stack;
                               
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
               """);
    }
}
