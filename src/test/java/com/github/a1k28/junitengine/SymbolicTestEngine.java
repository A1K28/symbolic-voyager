package com.github.a1k28.junitengine;

import com.github.a1k28.evoc.core.symbolicexecutor.SymbolicPathCarver;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.core.symbolicexecutor.model.VarType;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.helper.Logger;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SymbolicTestEngine implements TestEngine {
    private static final Logger log = Logger.getInstance(SymbolicTestEngine.class);
    private final Map<UniqueId, Set<Integer>> reachableCodes = new HashMap<>();

    static {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
        Runtime.getRuntime().addShutdownHook(new Thread(Z3Translator::close));
    }

    @Override
    public String getId() {
        return "symbolic-engine";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        EngineDescriptor rootDescriptor = new EngineDescriptor(uniqueId, "Symbolic Engine");

        // Example: Discover tests in a specific class
        discoveryRequest.getSelectorsByType(ClassSelector.class).forEach(selector -> {
            Class<?> testClass = selector.getJavaClass();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SymbolicTest.class)) {
                    UniqueId methodUniqueId = uniqueId.append("method", method.getName());
                    List<Integer> rcArr = Arrays.stream(method.getAnnotation(SymbolicTest.class).value())
                            .boxed().toList();
                    reachableCodes.put(methodUniqueId, new HashSet<>(rcArr));
                    rootDescriptor.addChild(new TestMethodTestDescriptor(methodUniqueId, testClass, method, null));
                }
            }
        });

        return rootDescriptor;
    }

    @Override
    public void execute(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        EngineExecutionListener listener = request.getEngineExecutionListener();

        listener.executionStarted(root);
        executeChildren(root, listener);
        listener.executionFinished(root, TestExecutionResult.successful());
    }

    private void executeChildren(TestDescriptor descriptor, EngineExecutionListener listener) {
        descriptor.getChildren().forEach(child -> {
            listener.executionStarted(child);
            if (child instanceof TestMethodTestDescriptor) {
                executeMethod((TestMethodTestDescriptor) child, listener);
            } else {
                executeChildren(child, listener);
            }
            listener.executionFinished(child, TestExecutionResult.successful());
        });
    }

    private void executeMethod(TestMethodTestDescriptor methodDescriptor, EngineExecutionListener listener) {
        try {
            UniqueId uniqueId = methodDescriptor.getUniqueId();
            assertMethodCorrectness(
                    methodDescriptor.getTestClass(),
                    methodDescriptor.getTestMethod(),
                    reachableCodes.get(uniqueId));
            // Handle the result as needed
        } catch (Throwable e) {
            listener.executionFinished(methodDescriptor, TestExecutionResult.failed(e));
        }
    }

    private void assertMethodCorrectness(Class testClass, Method testMethod, Set<Integer> reachableCodes)
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Set<Integer> reachedCodes = new HashSet<>();
        SatisfiableResults sr = new SymbolicPathCarver(
                testClass.getName(), testMethod.getName()).analyzeSymbolicPaths();
        Class<?>[] paramTypes = null;
        String[] paramNames = null;
        for (SatisfiableResult res : sr.getResults()) {
            if (paramTypes == null) {
                paramTypes = testMethod.getParameterTypes();
                paramNames = new String[paramTypes.length];
                int j = 0;
                for (SVarEvaluated key : res.getSymbolicParameterValues()) {
                    // TODO: handle mocked params
                    if (key.getSvar().getType() != VarType.PARAMETER)
                        throw new RuntimeException("MOCKED METHOD INBOUND");
                    paramNames[j] = key.getSvar().getName();
                    j++;
                }
            }
            Object[] parameters = new Object[paramNames.length];
            for (int j = 0; j < paramNames.length; j++)
                parameters[j] = parse(res.getParameter(paramNames[j]), paramTypes[j]);

            int expected = parse(res.getReturnValue().getEvaluated(), Integer.class);
            int actual = (int) testMethod.invoke(
                    testClass.getDeclaredConstructor().newInstance(), parameters);

            log.debug(String.format("Trying to assert parameters: %s" +
                            " with expected: %s & actual: %s codes",
                    Arrays.toString(parameters), expected, actual));

            assertEquals(expected, actual);
            assertTrue(reachableCodes.contains(expected));
            reachedCodes.add(expected);
        }
        reachableCodes.removeAll(reachedCodes);
        assertTrue(reachableCodes.isEmpty());
    }

    private <T> T parse(Object value, Class<T> type) {
        if (type == Integer.class || type == int.class)
            return (T) Integer.valueOf(value.toString());

        if (type == String.class) {
            String v = value.toString();
            if (value == null || value.toString().isBlank() || value.toString().length() < 2)
                return (T) value;
            return (T) value.toString().substring(1, value.toString().length()-1);
        }

        if (type == Map.class) {
            if (value == null) return (T) new HashMap<>();
            Map<?,?> map = (Map<?,?>) value;
            Map newMap = new HashMap<>();
            for (Map.Entry<?,?> entry : map.entrySet()) {
                newMap.put(parseString(entry.getKey()), parseString(entry.getValue()));
            }
            return (T) newMap;
        }

        throw new RuntimeException("Could not parse parameter: " + value + " with type: " + type);
    }

    private <T> T parseString(Object s) {
        if (s instanceof String v && v.length() >= 2) {
            if (v.startsWith("\"") && v.endsWith("\""))
                return (T) v.substring(1, v.length()-1);
        }
        return (T) s;
    }
}