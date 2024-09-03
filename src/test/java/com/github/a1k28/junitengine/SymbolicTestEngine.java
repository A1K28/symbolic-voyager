package com.github.a1k28.junitengine;

import com.github.a1k28.evoc.core.symbolicexecutor.SymbolTranslator;
import com.github.a1k28.evoc.core.symbolicexecutor.SymbolicExecutor;
import com.github.a1k28.evoc.core.symbolicexecutor.model.EvaluatedResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
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
            Z3Translator.initZ3(true);
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
        SatisfiableResults sr = new SymbolicExecutor(testClass, testMethod).analyzeSymbolicPaths();
        Map<SatisfiableResult, EvaluatedResult> evalMap = SymbolTranslator.translate(sr);
        for (SatisfiableResult satisfiableResult : sr.getResults()) {
            EvaluatedResult res = evalMap.get(satisfiableResult);
            Object[] parameters = res.getEvaluatedParameters();
            String paramsString = Arrays.toString(parameters);

            int expected = (int) res.getReturnValue();
            int actual = (int) testMethod.invoke(
                    testClass.getDeclaredConstructor().newInstance(), parameters);
            log.debug(String.format("Trying to assert parameters: %s" +
                            " with expected: %s & actual: %s codes",
                    paramsString, expected, actual));


            assertEquals(expected, actual);
            assertTrue(reachableCodes.contains(expected));
            reachedCodes.add(expected);
        }
        reachableCodes.removeAll(reachedCodes);
        assertTrue(reachableCodes.isEmpty());
    }
}