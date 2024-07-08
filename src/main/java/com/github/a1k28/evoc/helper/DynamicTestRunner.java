package com.github.a1k28.evoc.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.lang.reflect.Method;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamicTestRunner {
    private static final Logger log = Logger.getInstance(DynamicTestRunner.class);

    public static TestExecutionSummary runTestClass(Class<?> testClass, Method method) {
        log.info("Running test class: " + testClass.getName() + " method: " + method.getName());

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectMethod(testClass, method))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        log.info("Finished running test class: " + testClass.getName() + " method: "
                + method.getName() + " in: " + (summary.getTimeFinished()-summary.getTimeStarted()) + " ms");
        return summary;
    }

//    public static void printSummary(TestExecutionSummary summary) {
//        summary.printTo(new PrintWriter(System.out));
//    }
}