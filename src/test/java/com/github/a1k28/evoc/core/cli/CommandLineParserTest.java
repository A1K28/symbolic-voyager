package com.github.a1k28.evoc.core.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.cli.model.PropagationStrategy;
import com.github.a1k28.junitengine.BasicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.NoSuchElementException;

public class CommandLineParserTest {
    @BasicTest
    @DisplayName("test_cli_arg_parser_invalid_cmd")
    public void test_cli_arg_parser_invalid_cmd() {
        String cmd = """
                RANDOM TEXT BEFORE CMD -awdawd asd --AWDAWDAWD
                """;
        boolean thrown = false;
        try {
            CommandLineParser.setCLIOptions(cmd);
        } catch (NoSuchElementException ignored) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @BasicTest
    @DisplayName("test_cli_arg_parser_duplicate_cmd")
    public void test_cli_arg_parser_duplicate_cmd() {
        String cmd = """
                RANDOM TEXT BEFORE CMD 
                -tc   com.github.a1k28.evoc.core1, ge.asd.awd.123.core1
                -target-classes com.github.a1k28.evoc.core2,ge.asd.awd.123.core2
                -wc    com.github.a1k28.evoc.core3,ge.asd.awd.123.core3
                -wp com.github.a1k28.evoc.core4,ge.asd.awd.123.core4
                -bc   com.github.a1k28.evoc.core5    
                -bp com.github.a1k28.evoc.core6,ge.asd.awd.123.core6
                -pg PROPAGATE_POJO, PROPAGATE_STRUCT,MOCK_ALL_NOT_MODELLED,,,
                """;
        boolean thrown = false;
        try {
            CommandLineParser.setCLIOptions(cmd);
        } catch (RuntimeException ignored) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @BasicTest
    @DisplayName("test_cli_arg_parser_valid_shortcuts")
    public void test_cli_arg_parser_valid_shortcuts() {
        String cmd = """
                RANDOM TEXT BEFORE CMD 
                -tc   com.github.a1k28.evoc.core1, ge.asd.awd.123.core1
                -tp com.github.a1k28.evoc.core2,ge.asd.awd.123.core2
                -wc    com.github.a1k28.evoc.core3,ge.asd.awd.123.core3
                -wp com.github.a1k28.evoc.core4,ge.asd.awd.123.core4
                -bc   com.github.a1k28.evoc.core5    
                -bp com.github.a1k28.evoc.core6,ge.asd.awd.123.core6
                -pg PROPAGATE_POJO, PROPAGATE_STRUCT,MOCK_ALL_NOT_MODELLED,,,
                """;
        CommandLineParser.setCLIOptions(cmd);

        assertTrue(CLIOptions.targetClasses.contains("com.github.a1k28.evoc.core1"));
        assertTrue(CLIOptions.targetClasses.contains("ge.asd.awd.123.core1"));
        assertEquals(2, CLIOptions.targetClasses.size());
        assertTrue(CLIOptions.targetPackages.contains("com.github.a1k28.evoc.core2"));
        assertTrue(CLIOptions.targetPackages.contains("ge.asd.awd.123.core2"));
        assertEquals(2, CLIOptions.targetPackages.size());
        assertTrue(CLIOptions.whitelistedClasses.contains("com.github.a1k28.evoc.core3"));
        assertTrue(CLIOptions.whitelistedClasses.contains("ge.asd.awd.123.core3"));
        assertEquals(2, CLIOptions.whitelistedClasses.size());
        assertTrue(CLIOptions.whitelistedPackages.contains("com.github.a1k28.evoc.core4"));
        assertTrue(CLIOptions.whitelistedPackages.contains("ge.asd.awd.123.core4"));
        assertEquals(2, CLIOptions.whitelistedPackages.size());
        assertTrue(CLIOptions.blacklistedClasses.contains("com.github.a1k28.evoc.core5"));
        assertEquals(1, CLIOptions.blacklistedClasses.size());
        assertTrue(CLIOptions.blacklistedPackages.contains("com.github.a1k28.evoc.core6"));
        assertTrue(CLIOptions.blacklistedPackages.contains("ge.asd.awd.123.core6"));
        assertEquals(2, CLIOptions.blacklistedPackages.size());
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_POJO));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_STRUCT));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.MOCK_ALL_NOT_MODELLED));
        assertEquals(3, CLIOptions.propagationStrategies.size());
    }

    @BasicTest
    @DisplayName("test_cli_arg_parser_valid_commands")
    public void test_cli_arg_parser_valid_commands() {
        String cmd = """
                RANDOM TEXT BEFORE CMD 
                --target-classes   com.github.a1k28.evoc.core1, ge.asd.awd.123.core1
                --target-packages com.github.a1k28.evoc.core2,ge.asd.awd.123.core2
                --whitelisted-classes    com.github.a1k28.evoc.core3,ge.asd.awd.123.core3
                --whitelisted-packages com.github.a1k28.evoc.core4,ge.asd.awd.123.core4
                --blacklisted-classes   com.github.a1k28.evoc.core5    
                --blacklisted-packages com.github.a1k28.evoc.core6,ge.asd.awd.123.core6
                --propagation-strategies PROPAGATE_POJO, PROPAGATE_STRUCT,MOCK_ALL_NOT_MODELLED,,,
                """;
        CommandLineParser.setCLIOptions(cmd);

        assertTrue(CLIOptions.targetClasses.contains("com.github.a1k28.evoc.core1"));
        assertTrue(CLIOptions.targetClasses.contains("ge.asd.awd.123.core1"));
        assertEquals(2, CLIOptions.targetClasses.size());
        assertTrue(CLIOptions.targetPackages.contains("com.github.a1k28.evoc.core2"));
        assertTrue(CLIOptions.targetPackages.contains("ge.asd.awd.123.core2"));
        assertEquals(2, CLIOptions.targetPackages.size());
        assertTrue(CLIOptions.whitelistedClasses.contains("com.github.a1k28.evoc.core3"));
        assertTrue(CLIOptions.whitelistedClasses.contains("ge.asd.awd.123.core3"));
        assertEquals(2, CLIOptions.whitelistedClasses.size());
        assertTrue(CLIOptions.whitelistedPackages.contains("com.github.a1k28.evoc.core4"));
        assertTrue(CLIOptions.whitelistedPackages.contains("ge.asd.awd.123.core4"));
        assertEquals(2, CLIOptions.whitelistedPackages.size());
        assertTrue(CLIOptions.blacklistedClasses.contains("com.github.a1k28.evoc.core5"));
        assertEquals(1, CLIOptions.blacklistedClasses.size());
        assertTrue(CLIOptions.blacklistedPackages.contains("com.github.a1k28.evoc.core6"));
        assertTrue(CLIOptions.blacklistedPackages.contains("ge.asd.awd.123.core6"));
        assertEquals(2, CLIOptions.blacklistedPackages.size());
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_POJO));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_STRUCT));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.MOCK_ALL_NOT_MODELLED));
        assertEquals(3, CLIOptions.propagationStrategies.size());
    }

    @BasicTest
    @DisplayName("test_cli_arg_parser_valid_hybrid_commands")
    public void test_cli_arg_parser_valid_hybrid_commands() {
        String cmd = """
                RANDOM TEXT BEFORE CMD 
                -tc   com.github.a1k28.evoc.core1, ge.asd.awd.123.core1
                -tp com.github.a1k28.evoc.core2,ge.asd.awd.123.core2
                --whitelisted-classes    com.github.a1k28.evoc.core3,ge.asd.awd.123.core3
                -wp com.github.a1k28.evoc.core4,ge.asd.awd.123.core4
                --blacklisted-classes   com.github.a1k28.evoc.core5    
                -bp com.github.a1k28.evoc.core6,ge.asd.awd.123.core6
                -pg PROPAGATE_POJO, PROPAGATE_STRUCT,MOCK_ALL_NOT_MODELLED,,,
                """;
        CommandLineParser.setCLIOptions(cmd);

        assertTrue(CLIOptions.targetClasses.contains("com.github.a1k28.evoc.core1"));
        assertTrue(CLIOptions.targetClasses.contains("ge.asd.awd.123.core1"));
        assertEquals(2, CLIOptions.targetClasses.size());
        assertTrue(CLIOptions.targetPackages.contains("com.github.a1k28.evoc.core2"));
        assertTrue(CLIOptions.targetPackages.contains("ge.asd.awd.123.core2"));
        assertEquals(2, CLIOptions.targetPackages.size());
        assertTrue(CLIOptions.whitelistedClasses.contains("com.github.a1k28.evoc.core3"));
        assertTrue(CLIOptions.whitelistedClasses.contains("ge.asd.awd.123.core3"));
        assertEquals(2, CLIOptions.whitelistedClasses.size());
        assertTrue(CLIOptions.whitelistedPackages.contains("com.github.a1k28.evoc.core4"));
        assertTrue(CLIOptions.whitelistedPackages.contains("ge.asd.awd.123.core4"));
        assertEquals(2, CLIOptions.whitelistedPackages.size());
        assertTrue(CLIOptions.blacklistedClasses.contains("com.github.a1k28.evoc.core5"));
        assertEquals(1, CLIOptions.blacklistedClasses.size());
        assertTrue(CLIOptions.blacklistedPackages.contains("com.github.a1k28.evoc.core6"));
        assertTrue(CLIOptions.blacklistedPackages.contains("ge.asd.awd.123.core6"));
        assertEquals(2, CLIOptions.blacklistedPackages.size());
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_POJO));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.PROPAGATE_STRUCT));
        assertTrue(CLIOptions.propagationStrategies.contains(PropagationStrategy.MOCK_ALL_NOT_MODELLED));
        assertEquals(3, CLIOptions.propagationStrategies.size());
    }

}