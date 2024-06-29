package com.github.a1k28.evoc.core.evolution;

import com.github.a1k28.evoc.core.mutator.MutationService;
import com.github.a1k28.evoc.core.mutator.MutationServiceImpl;
import com.github.a1k28.evoc.helper.JavaASTHelper;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.RuntimeCompiler;
import com.github.a1k28.evoc.model.evolution.EvolutionProperties;
import com.github.a1k28.evoc.model.evolution.EvolutionStrategy;
import com.github.a1k28.evoc.model.evolution.MutationTestInfo;
import com.github.a1k28.evoc.model.evolution.TestNode;
import com.github.a1k28.evoc.model.mutator.DetectionMatrix;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class EvolutionServiceImpl implements EvolutionService {
    private final MutationService mutationService;
    private final EvolutionStrategy evolutionStrategy;
    private int strategyCounter = 0;
    private static final Logger log = Logger.getInstance(EvolutionServiceImpl.class);

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
        String prompt = Files.readString(Path.of(sourceCodePath), StandardCharsets.UTF_8);;
        String previousTests = null;
        String targetTestClass = null;
        float score = 0;
        int c = 0;
        boolean retry = false;
        do {
            c++;
            retry = false;
            if (previousTests != null) {
//                tests = JavaASTHelper.mergeJavaFiles(previousTests, tests);
            }
            try {
//                targetTestClass = RuntimeCompiler.compile(tests, classpath);
            } catch (Exception e) {
                prompt = e.getMessage();
                log.error("An exception has occurred when compiling: ", e);
                retry = true;
                continue;
            }
            DetectionMatrix detectionMatrix = mutationService
                    .generateMutants(sourceClass, targetTestClass);
            MutationTestInfo mutationTestInfo = calculateTestInfo(detectionMatrix);
//            tests = JavaASTHelper.keepWhitelistedTestsOnly(tests, mutationTestInfo.getTestsToKeep());
//            previousTests = tests;
            score = mutationTestInfo.getScore();
            prompt = getPrompt(mutationTestInfo.getSurvivingMutants());
            log.info("Next generation will be created based on the following mutators: " + prompt);
        } while (score < 1 && c < 5);

        String targetPath = targetDirectory+File.separator+targetTestClass.replace(".", File.separator)+".java";
        Files.write(Paths.get(targetPath), previousTests.getBytes());
    }

    private String getPrompt(List<String> survivingMutants) {
        List<String> p = survivingMutants.stream()
                .map(e -> e.split("\\."))
                .map(e -> e[e.length-1])
                .map(e -> e.substring(0, e.length()-1))
                .toList();
        return Arrays.toString(p.toArray())
                .replace("[", "\"")
                .replace("]", "\"");
//        String prompt = Arrays.toString(parentTests.toArray())
//                .replace("[", "\"")
//                .replace("]", "\"");
//        return prompt;
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

        List<String> testsToKeep = new ArrayList<>(Arrays.asList(data.getTests()));
        testsToKeep.removeAll(testsToRemove);

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
        score = score/matrix.length;

        // find parents for the next generation
        Queue<TestNode> pqt = new PriorityQueue<>(binaryRep.length,
                Comparator.comparingInt(TestNode::getMutationsKilled));
        for (int j = 0; j < matrix[0].length; j++) {
            if (testsToRemove.contains(data.getTests()[j])) continue;
            int m = 0;
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i][j]) m++;
            }
            pqt.add(new TestNode(data.getTests()[j], m));
        }
        List<String> parents = getParentsForNextGeneration(pqt).stream()
                .filter(e -> !e.contains(".") && !e.contains("/"))
                .collect(Collectors.toList());

        // find surviving mutator types
        List<String> survivingMutants = new ArrayList<>();
        for (int i = 0; i < matrix.length; i++) {
            int m = 0;
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j]) m++;
            }
            if (m == 0) survivingMutants.add(data.getMutants()[i]);
        }


        // create and return data object
        MutationTestInfo mutationTestInfo = new MutationTestInfo();
        mutationTestInfo.setScore(score);
        mutationTestInfo.setTestsToKeep(testsToKeep);
        mutationTestInfo.setParentTests(parents);
        mutationTestInfo.setSurvivingMutants(survivingMutants);

        return mutationTestInfo;
    }

    private List<String> getParentsForNextGeneration(Queue<TestNode> queue) {
        if (EvolutionStrategy.DYNAMIC == evolutionStrategy) {
            strategyCounter++;
            if (strategyCounter % 3 == 0)
                return getParentsForNextGeneration(queue, EvolutionStrategy.MAX_MIN);
            if (strategyCounter % 3 == 1)
                return getParentsForNextGeneration(queue, EvolutionStrategy.MAX_MAX);
            if (strategyCounter % 3 == 2)
                return getParentsForNextGeneration(queue, EvolutionStrategy.MAX_MAX_MIN);
        }
        return getParentsForNextGeneration(queue, evolutionStrategy);
    }

    private List<String> getParentsForNextGeneration(
            Queue<TestNode> queue, EvolutionStrategy evolutionStrategy) {
        List<String> parents = new ArrayList<>();
        if (EvolutionStrategy.MAX_MIN == evolutionStrategy) {
            addFromQueue(queue, parents);
            while (queue.size() > 1) {queue.poll();}
            addFromQueue(queue, parents);
        }
        if (EvolutionStrategy.MAX_MAX == evolutionStrategy) {
            addFromQueue(queue, parents);
            addFromQueue(queue, parents);
        }
        if (EvolutionStrategy.MAX_MAX_MIN == evolutionStrategy) {
            addFromQueue(queue, parents);
            addFromQueue(queue, parents);
            while (queue.size() > 1) {queue.poll();}
            addFromQueue(queue, parents);
        }
        return parents;
    }

    private void addFromQueue(Queue<TestNode> queue, List<String> target) {
        if (!queue.isEmpty()) target.add(Objects.requireNonNull(queue.poll()).getName());
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
}
