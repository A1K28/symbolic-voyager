package com.github.a1k28.evoc.helper;

import lombok.NoArgsConstructor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

@NoArgsConstructor
public class DynamicTestRunner {
    private static final Logger log = Logger.getInstance(DynamicTestRunner.class);

    public static TestExecutionSummary runTestClass(Class<?> testClass) {
        log.info("Running test: " + testClass.getName());
        long start = System.currentTimeMillis();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        long end = System.currentTimeMillis();
        log.info("Finished running test: " + testClass.getName() + " in: " + (end-start) + " ms");

        return listener.getSummary();
    }

//    public static void printSummary(TestExecutionSummary summary) {
//        summary.printTo(new PrintWriter(System.out));
//    }
}