package com.github.a1k28.evoc.core.branch;

import com.github.a1k28.evoc.helper.AgentLoader;
import com.github.a1k28.evoc.helper.DynamicTestRunner;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.List;

public class TestExecutorServiceImpl {
    public void executeTests(String sourceClassName, String testClassName) throws Exception {
        Class<?> testClass = Class.forName(testClassName);

        TestExecutionSummary summary = DynamicTestRunner.runTestClass(testClass);
    }

    public static void main(String[] args) throws Exception {
//        AgentLoader.load(List.of("com/github/a1k28/test/Stack."));
        TestExecutorServiceImpl service = new TestExecutorServiceImpl();
        service.executeTests("adw", "com.github.a1k28.test.StackTest");
    }
}
