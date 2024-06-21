package com.github.a1k28.core.evolution;

import com.github.a1k28.core.ai.AIService;
import com.github.a1k28.core.ai.ClaudeClient;
import com.github.a1k28.core.mutator.MutationService;
import com.github.a1k28.core.mutator.MutationServiceImpl;
import com.github.a1k28.helper.JavaASTParser;
import com.github.a1k28.helper.RuntimeCompiler;
import com.github.a1k28.model.evolution.EvolutionProperties;
import com.github.a1k28.model.evolution.EvolutionStrategy;
import com.github.a1k28.model.evolution.MutationTestInfo;
import com.github.a1k28.model.evolution.TestNode;
import com.github.a1k28.model.mutator.DetectionMatrix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EvolutionServiceImpl implements EvolutionService {
    private final MutationService mutationService;
    private final EvolutionStrategy evolutionStrategy;

    public EvolutionServiceImpl(Map<EvolutionProperties, Object> props) {
        this.mutationService = new MutationServiceImpl(props);
        this.evolutionStrategy = (EvolutionStrategy) props.getOrDefault(
                EvolutionProperties.EVOLUTION_STRATEGY, EvolutionStrategy.MAX_MIN);
    }

    @Override
    public void evolve(
            String classpath,
            String sourceCodePath,
            String sourceClass,
            String targetDirectory) throws Exception {
        AIService aiService = new ClaudeClient();
        String sourceCode = Files.readString(Path.of(sourceCodePath), StandardCharsets.UTF_8);

        String previousTests = null;
        float score = 0;
        int c = 0;
        do {
            String tests = aiService.generateTestClass(sourceCode);
            if (previousTests != null) {
                tests = JavaASTParser.mergeJavaFiles(previousTests, tests);
            }
            String targetTestClass = RuntimeCompiler.compile(tests, classpath);
            DetectionMatrix detectionMatrix = mutationService
                    .generateMutants(sourceClass, targetTestClass);
//            DetectionMatrix detectionMatrix = mutationService
//                    .generateMutants("com.github.a1k28.test.Stack", "StackTest");
            MutationTestInfo mutationTestInfo = calculateTestInfo(detectionMatrix);
            tests = JavaASTParser.keepWhitelistedTestsOnly(tests, mutationTestInfo.getTestsToKeep());
            previousTests = tests;
            c++;
        } while(score < 1 && c < 1);
    }

    private MutationTestInfo calculateTestInfo(DetectionMatrix data) {
        boolean[][] matrix = data.getMatrix();
        List<String> testsToRemove = new ArrayList<>();

        // find out which tests to remove
        String[] binaryRep = new String[matrix[0].length];
        for (int j = 0; j < matrix[0].length; j++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i][j]) sb.append("1");
                else sb.append("0");
            }
            binaryRep[j] = sb.toString();
        }

        for (int i = 0; i < binaryRep.length; i++) {
            for (int j = 0; j < binaryRep.length; j++) {
                if (i == j) continue;
                if (binaryRep[i].equals(and(binaryRep[i], binaryRep[j]))) {
                    testsToRemove.add(data.getTests()[i]);
                    break;
                }
            }
        }

        // calculate score
        float score = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j]) {
                    score++;
                    break;
                }
            }
        }
        score = score/matrix.length*100;

        // find parents for the next generation
        Queue<TestNode> pq = new PriorityQueue<>(binaryRep.length,
                Comparator.comparingInt(TestNode::getMutationsKilled));
        for (int j = 0; j < matrix[0].length; j++) {
            if (testsToRemove.contains(data.getTests()[j])) continue;
            int m = 0;
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i][j]) m++;
            }
            pq.add(new TestNode(data.getTests()[j], m));
        }
        List<String> parents = getParentsForNextGeneration(pq);

        List<String> testsToKeep = new ArrayList<>(Arrays.asList(data.getTests()));
        testsToKeep.removeAll(testsToRemove);

        // create and return data object
        MutationTestInfo mutationTestInfo = new MutationTestInfo();
        mutationTestInfo.setScore(score);
        mutationTestInfo.setTestsToKeep(testsToKeep);
        mutationTestInfo.setParentTests(parents);

        return mutationTestInfo;
    }

    private List<String> getParentsForNextGeneration(Queue<TestNode> queue) {
        List<String> parents = new ArrayList<>();
        if (EvolutionStrategy.MAX_MIN == evolutionStrategy) {
            parents.add(Objects.requireNonNull(queue.poll()).getName());
            while (queue.size() > 1) {queue.poll();}
            parents.add(Objects.requireNonNull(queue.poll()).getName());
        }
        if (EvolutionStrategy.MAX_MAX == evolutionStrategy) {
            parents.add(Objects.requireNonNull(queue.poll()).getName());
            parents.add(Objects.requireNonNull(queue.poll()).getName());
        }
        if (EvolutionStrategy.MAX_MAX_MIN == evolutionStrategy) {
            parents.add(Objects.requireNonNull(queue.poll()).getName());
            parents.add(Objects.requireNonNull(queue.poll()).getName());
            while (queue.size() > 1) {queue.poll();}
            parents.add(Objects.requireNonNull(queue.poll()).getName());
        }
        return parents;
    }

    private String and(String a, String b) {
        assert a.length() == b.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) == '1' && b.charAt(i) == '1') sb.append('1');
            else sb.append('0');
        }
        return sb.toString();
    }

    // calculates how many mutations have been covered (as a percentage between 0 & 1).
    private float calculateMatrixScore(DetectionMatrix matrix) {
        return 1f;
    }
}
