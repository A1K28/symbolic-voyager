package com.github.a1k28.evoc.core.cli.model;

import java.util.Set;

public class CLIOptions {
    public static Set<String> targetClasses = Set.of();
    public static Set<String> targetPackages = Set.of();
    public static Set<String> whitelistedClasses = Set.of();
    public static Set<String> whitelistedPackages = Set.of();
    public static Set<String> blacklistedPackages = Set.of();
    public static Set<String> blacklistedClasses = Set.of();
    public static Set<PropagationStrategy> propagationStrategies;

    public static boolean shouldUsePackage(String pckg) {
        for (String blacklistedPackage : blacklistedPackages)
            if (pckg.contains(blacklistedPackage)) return false;
        for (String blacklistedClass : blacklistedClasses)
            if (pckg.equals(blacklistedClass)) return false;
        for (String whitelistedPackage : whitelistedPackages)
            if (pckg.contains(whitelistedPackage)) return true;
        for (String whitelistedClass : whitelistedClasses)
            if (pckg.equals(whitelistedClass)) return true;
        return false;
    }
}
