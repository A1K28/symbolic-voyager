package com.github.a1k28.evoc.core.cli;

import com.github.a1k28.evoc.core.assembler.JUnitTestAssembler;
import com.github.a1k28.evoc.core.assembler.model.TestGeneratorModel;
import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.cli.model.CommandFlag;
import com.github.a1k28.evoc.core.symbolicexecutor.SymbolTranslator;
import com.github.a1k28.evoc.core.symbolicexecutor.SymbolicExecutor;
import com.github.a1k28.evoc.core.symbolicexecutor.model.ParsedResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.SatisfiableResults;
import com.github.a1k28.supermock.Parser;
import org.apache.commons.cli.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class CommandLineRunner {
    public static void main(String[] args) throws Exception {
        parseCliArgs(args);
        Class clazz = Class.forName(CLIOptions.targetClass);

        promptUserForPropagation();

        SymbolicExecutor symbolicExecutor = new SymbolicExecutor();
        List<TestGeneratorModel> testGeneratorModels = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            // skip if void
            if ("void".equals(method.getReturnType().getName())) continue;

            // skip if non-public
            if (!Modifier.isPublic(method.getModifiers())) continue;

            symbolicExecutor.refresh();
            SatisfiableResults sr = symbolicExecutor.analyzeSymbolicPaths(method);
            Map<SatisfiableResult, ParsedResult> evalMap = SymbolTranslator.parse(sr);

            Set<String> set = new HashSet<>();
            for (SatisfiableResult satisfiableResult : sr.getResults()) {
                ParsedResult res = evalMap.get(satisfiableResult);
                String uniqueKey = Parser.serialize(res.getParsedReturnValue());
                if (set.contains(uniqueKey)) continue;
                set.add(uniqueKey);
                testGeneratorModels.add(new TestGeneratorModel(method, res));

            }
        }

        JUnitTestAssembler.assembleTest(clazz, testGeneratorModels);

//        JUnitTestAssembler.assembleTest(clazz,
//                method.getName(),
//                res.getParsedReturnValue(),
//                Arrays.asList(res.getParsedParameters()),
//                res.getMethodMockValues());
    }

    private static void promptUserForPropagation() throws ClassNotFoundException {
        if (CLIOptions.mockableClasses.isEmpty() || CLIOptions.mockablePackages.isEmpty()) {
            Set<String> variableTypes = LocalTypeExtractor
                    .extract(Class.forName(CLIOptions.targetClass)).stream()
                    .filter(CommandLineRunner::shouldConsiderType)
                    .collect(Collectors.toSet());
            if (variableTypes.isEmpty()) return;

            try (final Scanner reader = new Scanner(System.in)) {
                System.out.println("You have not provided any classes" +
                        " that should be targeted by supermock." +
                        " Would you like me to suggest possible classes? (Y/n)");
                String res = reader.next();
                if (!res.isBlank() && !res.equalsIgnoreCase("Y")) return;

                for (String type : variableTypes) {
                    System.out.println("Mock: " + type + "? (y/N)");
                    res = reader.next();
                    if (!res.equalsIgnoreCase("Y")) continue;
                    CLIOptions.set(CommandFlag.MOCKABLE_CLASSES, type);
                }
            }
        }
    }

    private static boolean shouldConsiderType(String type) {
        if (type.startsWith("java.")) return false;
        if (type.equals("byte")
                || type.equals("short")
                || type.equals("int")
                || type.equals("long")
                || type.equals("float")
                || type.equals("double")
                || type.equals("char")
                || type.equals("boolean")) return false;
        return true;
    }

    private static void parseCliArgs(String[] args) {
        Options options = new Options();

        for (CommandFlag flag : CommandFlag.values()) {
            Option input = new Option(flag.option(), flag.longOption(),
                    flag.hasArg(), flag.description());
            input.setRequired(false);
            options.addOption(input);
        }

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        for (CommandFlag flag : CommandFlag.values()) {
            String value = cmd.getOptionValue(flag.option());
            CLIOptions.set(flag, value);
        }
    }
}
