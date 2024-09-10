package com.github.a1k28.junitengine;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.symbolicexecutor.SymbolTranslator;
import com.github.a1k28.evoc.core.symbolicexecutor.SymbolicExecutor;
import com.github.a1k28.evoc.core.symbolicexecutor.model.ParsedResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVar;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.SVarEvaluated;
import com.github.a1k28.evoc.helper.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.a1k28.evoc.helper.SootHelper.translateField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SymbolicTestEngine implements TestEngine {
    private static final Logger log = Logger.getInstance(SymbolicTestEngine.class);
    private final Map<UniqueId, Set<Integer>> reachableCodes = new HashMap<>();
    private final SymbolicExecutor symbolicExecutor = new SymbolicExecutor();

    static {
        CLIOptions.whitelistedPackages = Set.of("com.github.a1k28.evoc.core");
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
                } else if (method.isAnnotationPresent(BasicTest.class)) {
                    UniqueId methodUniqueId = uniqueId.append("junit:method", method.getName());
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
            if (uniqueId.getLastSegment().getType().startsWith("junit:")) {
                assertMethodCorrectness(methodDescriptor.getTestClass(), methodDescriptor.getTestMethod());
            } else {
                assertMethodCorrectness(
                        methodDescriptor.getTestClass(),
                        methodDescriptor.getTestMethod(),
                        reachableCodes.get(uniqueId));
            }
        } catch (Throwable e) {
            listener.executionFinished(methodDescriptor, TestExecutionResult.failed(e));
        }
    }

    private void assertMethodCorrectness(Class testClass, Method testMethod)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Constructor constructor = testClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        testMethod.invoke(instance);
    }


    private void assertMethodCorrectness(Class testClass, Method testMethod, Set<Integer> reachableCodes)
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Set<Integer> reachedCodes = new HashSet<>();
        symbolicExecutor.refresh();
        SatisfiableResults sr = symbolicExecutor.analyzeSymbolicPaths(testMethod);
        Map<SatisfiableResult, ParsedResult> evalMap = SymbolTranslator.parse(sr);
        for (SatisfiableResult satisfiableResult : sr.getResults()) {
            ParsedResult res = evalMap.get(satisfiableResult);
            Object[] parameters = res.getParsedParameters();
            String paramsString = Arrays.toString(parameters);
            Object instance = testClass.getDeclaredConstructor().newInstance();
            overwriteFields(testClass, instance, res.getParsedFields());

            int expected = (int) res.getParsedReturnValue();
            int actual = (int) testMethod.invoke(instance, parameters);
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

    private void overwriteFields(Class testClass, Object instance, List<SVarEvaluated> evaluatedFields)
            throws IllegalAccessException {
        Field[] declaredFields = testClass.getDeclaredFields();
        outer: for (SVarEvaluated sVarEvaluated : evaluatedFields) {
            for (Field declaredField : declaredFields) {
                if (equalsField(sVarEvaluated.getSvar(), declaredField)) {
                    declaredField.setAccessible(true);
                    declaredField.set(instance, sVarEvaluated.getEvaluated());
                    continue outer;
                }
            }
        }
    }

    private boolean equalsField(SVar sVar, Field field) {
        return translateField(field).equals(sVar.getName());
//        JInstanceFieldRef fieldRef = (JInstanceFieldRef) sVar.getValue();
//        if (!field.getName().equals(fieldRef.getFieldSignature().getName())) return false;
//        if (!field.getType().equals(sVar.getClassType())) return false;
//        if (!field.getDeclaringClass().getName().equals(
//                fieldRef.getFieldSignature().getDeclClassType().toString())) return false;
//        return true;
    }
}