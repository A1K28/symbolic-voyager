package com.github.a1k28.core.mutator;

import com.github.a1k28.helper.Logger;
import com.github.a1k28.model.mutator.DetectionMatrix;
import com.github.a1k28.model.mutator.MutatorProperties;
import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.MutationMetaData;
import org.pitest.mutationtest.MutationResult;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MutationServiceImpl implements MutationService {
    private final boolean removeSucceedingTests;
    private static final Pattern pattern = Pattern.compile("\\[(.*?)\\]");
    private static final Logger log = Logger.getInstance(MutationServiceImpl.class);

    public MutationServiceImpl() {
        this(new HashMap<>());
    }

    public MutationServiceImpl(Map<MutatorProperties, Boolean> props) {
        this.removeSucceedingTests = props.getOrDefault(MutatorProperties.REMOVE_SUCCEEDING_TESTS, false);
    }

    @Override
    public DetectionMatrix generateMutants(String targetClass, String targetTestClass) {
        final PluginServices pluginServices = PluginServices.makeForContextLoader();
        final ReportOptions reportOptions = new ReportOptions();
        final TestGroupConfig testGroupConfig = new TestGroupConfig();

        final String tempDir = System.getProperty("java.io.tmpdir")
                + "evolutionary-copilot-tests-cache";
        log.info("temp directory: " + tempDir);

        reportOptions.setReportDir(tempDir);
        reportOptions.setSourceDirs(List.of(Path.of(tempDir)));
        reportOptions.setTargetClasses(List.of(targetClass));
        reportOptions.setTargetTests(predicateFor(targetTestClass));
        reportOptions.setExcludedRunners(Collections.emptyList());
        reportOptions.setGroupConfig(testGroupConfig);

        final CombinedStatistics stats = runReport(reportOptions, pluginServices);

        return map(stats.getMutationMetaData().get(0));
    }

    private DetectionMatrix map(MutationMetaData mutationMetaData) {
        List<MutationResult> mutationResults = (List<MutationResult>) mutationMetaData.getMutations();

        List<String> succeedingTests;
        if (removeSucceedingTests) {
            succeedingTests = mutationResults.stream()
                    .map(MutationResult::getSucceedingTests)
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();
        } else {
            succeedingTests = Collections.emptyList();
        }

        List<TestInfo> testInfos = mutationResults.stream()
                .map(e -> e.getDetails().getTestsInOrder())
                .flatMap(Collection::stream)
                .distinct()
                .filter(e -> !succeedingTests.contains(e.getName()))
                .toList();

        String[] tests = testInfos.stream()
                .map(e -> extractMethodName(e.getName()))
                .toList()
                .toArray(new String[0]);
        String[] mutants = mutationResults.stream()
                .map(e -> e.getDetails().getId().toString())
                .toList()
                .toArray(new String[0]);
        boolean[][] matrix = new boolean[mutants.length][tests.length];

        for (int m = 0; m < mutants.length; m++) {
            List<String> killingTests = mutationResults.get(m).getKillingTests();
            for (int t = 0; t < tests.length; t++) {
                if (killingTests.contains(testInfos.get(t).getName())) {
                    matrix[m][t] = true;
                }
            }
        }

        DetectionMatrix detectionMatrix = new DetectionMatrix();
        detectionMatrix.setTests(tests);
        detectionMatrix.setMutants(mutants);
        detectionMatrix.setMatrix(matrix);
        return detectionMatrix;
    }

    private String extractMethodName(String fullName) {
        Matcher m = pattern.matcher(fullName);
        String lastMatch = null;
        while(m.find()) {
            lastMatch = m.group(1);
        }
        if (lastMatch != null && lastMatch.contains("method:")) {
            lastMatch = lastMatch.split(":")[1];
            return lastMatch.substring(0, lastMatch.length()-2);
        }
        return fullName;
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
