package com.github.a1k28.symvoyager.core.cli;

import com.github.a1k28.symvoyager.core.assembler.JUnitTestAssembler;
import com.github.a1k28.symvoyager.core.assembler.model.TestGeneratorModel;
import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.cli.model.CommandFlag;
import com.github.a1k28.symvoyager.core.symbolicexecutor.SymbolTranslator;
import com.github.a1k28.symvoyager.core.symbolicexecutor.SymbolicExecutor;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.ParsedResult;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SatisfiableResult;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SatisfiableResults;
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
            if (Void.class.equals(method.getReturnType())) continue;

            // skip if non-public
            if (!Modifier.isPublic(method.getModifiers())) continue;

            symbolicExecutor.refresh();
            SatisfiableResults sr = symbolicExecutor.analyzeSymbolicPaths(method);
            Map<SatisfiableResult, ParsedResult> evalMap = SymbolTranslator.parse(sr);

            // TODO: reduce test cases whose paths are covered by other test cases
            for (SatisfiableResult satisfiableResult : sr.getResults()) {
                ParsedResult res = evalMap.get(satisfiableResult);
                testGeneratorModels.add(new TestGeneratorModel(method, res));

            }
        }

        JUnitTestAssembler.assembleTest(clazz, testGeneratorModels);
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
                        " that should be targeted by supermock (enabled by default)." +
                        " Would you like me to suggest possible classes? (y/N)");
                String res = reader.next();
                if (!res.equalsIgnoreCase("Y")) {
                    // if not, then mock all
                    for (String type : variableTypes) {
                        CLIOptions.set(CommandFlag.MOCKABLE_CLASSES, type);
                    }
                    return;
                };

                int i = 0;
                for (String type : variableTypes) {
                    i++;
                    System.out.println("(" + i + "/" + variableTypes.size() + ")"
                            + " Mock: " + type + "? (y/N)");
                    res = reader.next();
                    if (res.equalsIgnoreCase("Y")) {
                        CLIOptions.set(CommandFlag.MOCKABLE_CLASSES, type);
                    } else {
                        CLIOptions.set(CommandFlag.WHITELISTED_CLASSES, type);
                    }
                }
            }
        }
    }

    private static boolean shouldConsiderType(String type) {
        if (CLIOptions.shouldPropagate(type)) return false;
        if ("void".equals(type)) return false;
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
