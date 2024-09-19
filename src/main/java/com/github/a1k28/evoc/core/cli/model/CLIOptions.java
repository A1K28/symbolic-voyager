package com.github.a1k28.evoc.core.cli.model;

import java.util.Set;

public class CLIOptions {
    public static String targetClass = null;
    public static Set<String> whitelistedClasses = Set.of();
    public static Set<String> whitelistedPackages = Set.of();
    public static Set<String> mockableClasses = Set.of();
    public static Set<String> mockablePackages = Set.of();
    public static Integer gotoLimit = 20;

    public static boolean shouldPropagate(String pckg) {
        if (targetClass.startsWith(pckg)) return true;
        for (String mockablePackage : mockablePackages)
            if (pckg.startsWith(mockablePackage)) return false;
        for (String mockableClass : mockableClasses)
            if (pckg.equals(mockableClass)) return false;
        for (String whitelistedPackage : whitelistedPackages)
            if (pckg.startsWith(whitelistedPackage)) return true;
        for (String whitelistedClass : whitelistedClasses)
            if (pckg.equals(whitelistedClass)) return true;
        return false;
    }

    public static boolean shouldMock(String pckg) {
        if (targetClass.startsWith(pckg)) return false;
        for (String mockablePackage : mockablePackages)
            if (pckg.startsWith(mockablePackage)) return true;
        for (String mockableClass : mockableClasses)
            if (pckg.equals(mockableClass)) return true;
        return false;
    }

    public static void set(CommandFlag flag, String value) {
        if (value == null) return;
        switch (flag) {
            case TARGET_CLASS -> targetClass = value;
            case WHITELISTED_CLASSES -> {
                for (String v : split(value))
                    whitelistedClasses.add(v.strip());
            }
            case WHITELISTED_PACKAGES -> {
                for (String v : split(value))
                    whitelistedPackages.add(v.strip());
            }
            case MOCKABLE_CLASSES -> {
                for (String v : split(value))
                    mockableClasses.add(v.strip());
            }
            case MOCKABLE_PACKAGES -> {
                for (String v : split(value))
                    mockablePackages.add(v.strip());
            }
            case RECURSION_LIMIT -> gotoLimit = Integer.parseInt(value);
        }
    }

    private static String[] split(String value) {
        return value.strip()
                .replace("\n","")
                .replace("\r","")
                .split(",");
    }
}
