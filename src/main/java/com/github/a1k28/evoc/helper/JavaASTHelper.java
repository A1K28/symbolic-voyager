package com.github.a1k28.evoc.helper;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class JavaASTHelper {
    private static final Logger log = Logger.getInstance(JavaASTHelper.class);

    public static String mergeJavaFiles(String file1, String file2) {
        CompilationUnit cu1 = StaticJavaParser.parse(file1);
        CompilationUnit cu2 = StaticJavaParser.parse(file2);

        ClassOrInterfaceDeclaration class1 = cu1.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in file 1: " + file1));
        ClassOrInterfaceDeclaration class2 = cu2.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in file 2: " + file2));

        // add imports from cu2 to cu1
        List<ImportDeclaration> class1Imports = cu1.getImports();
        List<ImportDeclaration> class2Imports = cu2.getImports();
        for (ImportDeclaration importDeclaration : class2Imports) {
            if (class1Imports.contains(importDeclaration)) continue;
            cu1.addImport(importDeclaration);
        }

        // add fields from class2 to class1
        List<FieldDeclaration> class1Fields = class1.getFields();
        List<String> class1FieldNames = class1Fields.stream()
                .map(e -> e.getVariables().getFirst().get().getNameAsString())
                .toList();
        List<FieldDeclaration> class2Fields = class2.getFields();
        for (FieldDeclaration fieldDeclaration : class2Fields) {
            if (class1Fields.contains(fieldDeclaration)) continue;
            VariableDeclarator variableDeclarator = fieldDeclaration.getVariables().getFirst().get();
            if (class1FieldNames.contains(variableDeclarator.getNameAsString())) continue;
            if (variableDeclarator.getInitializer().isPresent()) {
                class1.addFieldWithInitializer(fieldDeclaration.getElementType(),
                        variableDeclarator.getNameAsString(),
                        variableDeclarator.getInitializer().get(),
                        fieldDeclaration.getModifiers().stream()
                                .map(Modifier::getKeyword).toList()
                                .toArray(new Modifier.Keyword[0]));
            } else {
                class1.addField(fieldDeclaration.getElementType(),
                        fieldDeclaration.getVariables().getFirst().get().getNameAsString(),
                        fieldDeclaration.getModifiers().stream()
                                .map(Modifier::getKeyword).toList()
                                .toArray(new Modifier.Keyword[0]));
            }
        }

        // add methods from class2 to class1
        List<MethodDeclaration> class2Methods = class2.getMethods();
        List<String> signatures = class1.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(e -> e.asMethodDeclaration().getSignature().asString())
                .collect(Collectors.toList());
        for (MethodDeclaration method : class2Methods) {
            if (class1.getMembers().contains(method)) continue;

            String sig = method.getSignature().asString();
            if (signatures.contains(sig)) { // remove first declaration if duplicates found
                class1.getMembers().stream()
                        .filter(BodyDeclaration::isMethodDeclaration)
                        .filter(e -> sig.equals(e.asMethodDeclaration().getSignature().asString()))
                        .findFirst().ifPresent(Node::remove);
            } else {
                signatures.add(sig);
            }
            class1.addMember(method);
        }

        // Return the merged class as a string
        return cu1.toString();
    }

    public static String keepWhitelistedTestsOnly(String code, List<String> whitelist) {
        CompilationUnit cu = StaticJavaParser.parse(code);
        ClassOrInterfaceDeclaration cl = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in code: " + code));
        for (MethodDeclaration method : cl.getMethods()) {
            List<String> annotations = method.getAnnotations().stream()
                    .map(NodeWithName::getNameAsString).toList();
            if (!annotations.contains("Test")) continue;
            if (whitelist.contains(method.getNameAsString())) continue;
            if (!cl.remove(method)) {
                log.warn("Could not remove method: " + method.getNameAsString());
            } else {
                log.info("Successfully removed method: " + method.getNameAsString());
            }
        }
        return cu.toString();
    }

    public static void main(String[] args) {
        String file1 = """
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                                
                import com.github.a1k28.Stack;
                               
                import java.util.EmptyStackException;
                               
                import static org.junit.jupiter.api.Assertions.*;
                               
                class StackTest {
                    private Stack<String> stack;
                    private String test2;
                               
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
                }
               """;
        String file2 = """
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                import org.pitest.coverage.TestInfo;
                
                import com.github.a1k28.Stack;
                               
                import java.util.EmptyStackException;
                               
                import static org.junit.jupiter.api.Assertions.*;
                               
                class StackTest {
                    private Stack<String> stack;
                    
                    private String test = "asdads";
                               
                    @BeforeEach
                    void setUp() {
                        String asd = "asdawdawd";
                        stack = new Stack<>();
                    }
                
                    @Test
                    void testPopEmptyStack() {
                        assertThrows(EmptyStackException.class, () -> stack.pop());
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
                """;

        String mergedFile = mergeJavaFiles(file1, file2);
        log.info(mergedFile);
    }
}
