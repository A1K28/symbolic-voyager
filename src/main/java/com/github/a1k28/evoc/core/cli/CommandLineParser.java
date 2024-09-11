package com.github.a1k28.evoc.core.cli;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.cli.model.CommandFlag;
import com.github.a1k28.evoc.core.cli.model.PropagationStrategy;
import com.github.a1k28.evoc.helper.Logger;

import java.util.HashSet;
import java.util.Set;

public class CommandLineParser {
    private static final Logger log = Logger.getInstance(CommandLineParser.class);

    public static void setCLIOptions(String cmd) {
        CommandFlag lastFlag = null;
        cmd = cmd.replace("\n","")
                .replace("\r","");
        cmd = cmd.substring(cmd.indexOf("-"));
        StringBuilder sb = new StringBuilder();
        Set<CommandFlag> flags = new HashSet<>();
        boolean isShortCut = true;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == ' ' && sb.isEmpty() && lastFlag == null)
                continue;

            if (c == '-' && sb.isEmpty()) {
                lastFlag = null;
                if (cmd.charAt(i+1) == '-') {
                    isShortCut = false;
                    i++;
                } else {
                    isShortCut = true;
                }
                continue;
            }

            if (c == ' ' && lastFlag == null) {
                lastFlag = getFlag(sb.toString(), isShortCut);
                if (flags.contains(lastFlag))
                    throw new RuntimeException("Duplicate flag: " + lastFlag);
                flags.add(lastFlag);
                sb = new StringBuilder();
            } else if (c == '-' && lastFlag != null) {
                i--;
                setCommand(lastFlag, sb.toString());
                lastFlag = null;
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        if (lastFlag != null)
            setCommand(lastFlag, sb.toString());
    }

    private static void setCommand(CommandFlag flag, String cmd) {
        if (CommandFlag.TARGET_CLASSES == flag) {
            CLIOptions.targetClasses = Set.of(split(cmd));
        } else if (CommandFlag.TARGET_PACKAGES == flag) {
            CLIOptions.targetPackages = Set.of(split(cmd));
        } else if (CommandFlag.WHITELISTED_CLASSES == flag) {
            CLIOptions.whitelistedClasses = Set.of(split(cmd));
        } else if (CommandFlag.WHITELISTED_PACKAGES == flag) {
            CLIOptions.whitelistedPackages = Set.of(split(cmd));
        } else if (CommandFlag.BLACKLISTED_CLASSES == flag) {
            CLIOptions.blacklistedClasses = Set.of(split(cmd));
        } else if (CommandFlag.BLACKLISTED_PACKAGES == flag) {
            CLIOptions.blacklistedPackages = Set.of(split(cmd));
        } else if (CommandFlag.PROPAGATION_STRATEGY == flag) {
            Set<PropagationStrategy> propagationStrategies = new HashSet<>();
            for (String val : split(cmd))
                propagationStrategies.add(PropagationStrategy.valueOf(val));
            CLIOptions.propagationStrategies = propagationStrategies;
        } else {
            log.warn("Invalid command: " + flag);
        }
    }

    private static String[] split(String cmd) {
        return cmd.replace(" ", "").split(",");
    }

    private static CommandFlag getFlag(String flagString, boolean isShortCut) {
        return CommandFlag.findByFlag(flagString, isShortCut).orElseThrow();
    }
}
