package com.github.a1k28.core.mutator;

import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.AnalysisResult;
import org.pitest.mutationtest.tooling.CombinedStatistics;
import org.pitest.mutationtest.tooling.EntryPoint;
import org.pitest.testapi.TestGroupConfig;
import org.pitest.util.Glob;
import org.pitest.util.Unchecked;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class MutationServiceImpl implements MutationService {
    public void generateMutants() {
        final PluginServices pluginServices = PluginServices.makeForContextLoader();
        final ReportOptions reportOptions = new ReportOptions();
        TestGroupConfig testGroupConfig = new TestGroupConfig();

        reportOptions.setReportDir("/Users/ak/Desktop/LL-test");
        reportOptions.setSourceDirs(List.of(Path.of("/Users/ak/Desktop/LL-test")));
        reportOptions.setTargetClasses(List.of("com.github.a1k28.LinkedList"));
        reportOptions.setTargetTests(predicateFor("com.github.a1k28.LinkedListTest"));
        reportOptions.setExcludedRunners(Collections.emptyList());
        reportOptions.setGroupConfig(testGroupConfig);

        final ParseResult pr = new ParseResult(reportOptions, null);
        final ReportOptions data = pr.getOptions();
        final CombinedStatistics stats = runReport(data, pluginServices);

        // Generate and store mutation tests in memory
//        Map<String, CombinedStatistics> results = entryPoint.execute(settingsFactory.getSettings());
//
//        // Reuse the stored mutation tests
//        Map<String, CombinedStatistics> reusedResults = entryPoint.execute(settingsFactory.getSettings());

        // Print the combined statistics
//        CombinedStatistics stats = results.values().iterator().next();
//        System.out.println("Mutations: " + stats.getTotalMutations());
//        System.out.println("Killed: " + stats.getTotalDetectedMutations());
//        System.out.println("Undetected: " + stats.getTotalUndetectedMutations());
    }

    protected Collection<Predicate<String>> predicateFor(final String... glob) {
        return Glob.toGlobPredicates(Arrays.asList(glob));
    }

    private static CombinedStatistics runReport(ReportOptions data,
                                                PluginServices plugins) {
        final EntryPoint entryPoint = new EntryPoint();
        final AnalysisResult result = entryPoint.execute(null, data, plugins,
                new HashMap<>());
        if (result.getError().isPresent()) {
            throw Unchecked.translateCheckedException(result.getError().get());
        }
        return result.getStatistics().get();
    }
}
