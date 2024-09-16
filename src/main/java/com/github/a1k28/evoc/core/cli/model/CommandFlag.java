package com.github.a1k28.evoc.core.cli.model;

import com.github.a1k28.evoc.helper.Logger;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public enum CommandFlag {
    TARGET_CLASSES("tc", "target-classes"),
    TARGET_PACKAGES("tp", "target-packages"),
    WHITELISTED_CLASSES("wc", "whitelisted-classes"),
    WHITELISTED_PACKAGES("wp", "whitelisted-packages"),
    MOCKABLE_CLASSES("mc", "mockable-classes"),
    MOCKABLE_PACKAGES("mp", "mockable-packages"),
    PROPAGATION_STRATEGY("pg", "propagation-strategies");

    private static final Logger log = Logger.getInstance(CommandFlag.class);

    private final String shortValue;
    private final String fullValue;

    public static Optional<CommandFlag> findByFlag(String cmd, boolean isShortCut) {
        for (CommandFlag commandFlag : CommandFlag.values()) {
            if (isShortCut && commandFlag.shortValue.equals(cmd)) return Optional.of(commandFlag);
            if (!isShortCut && commandFlag.fullValue.equals(cmd)) return Optional.of(commandFlag);
        }
        log.error("Could not find flag with command: " + cmd + " & isShortCut: " + isShortCut);
        return Optional.empty();
    }
}
