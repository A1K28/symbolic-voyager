package com.github.a1k28.evoc.core.cli;

import com.github.a1k28.evoc.core.cli.model.CLIOptions;
import com.github.a1k28.evoc.core.cli.model.CommandFlag;
import com.github.a1k28.evoc.core.cli.model.PropagationStrategy;

import java.util.HashSet;
import java.util.Set;

public class CommandLineParser {
    public static void setCLIOptions(String cmd) {
        CommandFlag lastFlag = null;
        cmd = cmd.replace("\n","")
                .replace("\r","");
        cmd = cmd.substring(cmd.indexOf("-"));
        StringBuilder sb = new StringBuilder();
        boolean isShortCut = false;
        boolean lastWasDash = false;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == '-') {
                if (lastFlag != null) {
                    setCommand(lastFlag, sb.toString());
                    sb = new StringBuilder();
                }
                if (lastWasDash) isShortCut = true;
                lastWasDash = true;
                lastFlag = null;
            } else {
                lastWasDash = false;
                if (c == ' ') {
                    if (lastFlag == null) {
                        lastFlag = getFlag(sb.toString(), isShortCut);
                        sb = new StringBuilder();
                        isShortCut = false;
                    }
                } else {
                    sb.append(c);
                }
            }
        }
        if (lastFlag != null)
            setCommand(lastFlag, sb.toString());
    }

    private static void setCommand(CommandFlag flag, String cmd) {
        if (CommandFlag.TARGET_CLASSES == flag) {
            CLIOptions.targetClasses = Set.of(split(cmd));
        }
        if (CommandFlag.TARGET_PACKAGES == flag) {
            CLIOptions.targetPackages = Set.of(split(cmd));
        }
        if (CommandFlag.WHITELISTED_CLASSES == flag) {
            CLIOptions.whitelistedClasses = Set.of(split(cmd));
        }
        if (CommandFlag.WHITELISTED_PACKAGES == flag) {
            CLIOptions.whitelistedPackages = Set.of(split(cmd));
        }
        if (CommandFlag.BLACKLISTED_CLASSES == flag) {
            CLIOptions.blacklistedClasses = Set.of(split(cmd));
        }
        if (CommandFlag.BLACKLISTED_PACKAGES == flag) {
            CLIOptions.blacklistedPackages = Set.of(split(cmd));
        }
        if (CommandFlag.PROPAGATION_STRATEGY == flag) {
            Set<PropagationStrategy> propagationStrategies = new HashSet<>();
            for (String val : split(cmd))
                propagationStrategies.add(PropagationStrategy.valueOf(val));
            CLIOptions.propagationStrategies = propagationStrategies;
        }
    }

    private static String[] split(String cmd) {
        return cmd.replace(" ", "").split(",");
    }

    private static CommandFlag getFlag(String flagString, boolean isShortCut) {
        return CommandFlag.findByFlag(flagString, isShortCut).orElseThrow();
    }
}
