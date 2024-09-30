package com.github.a1k28.symvoyager.core.cli.model;

import com.github.a1k28.symvoyager.helper.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CLIOptions {
    public static String targetClass = null;
    public static Set<String> whitelistedClasses = new HashSet<>();
    public static Set<String> whitelistedPackages = new HashSet<>();
    public static Set<String> mockableClasses = new HashSet<>();
    public static Set<String> mockablePackages = new HashSet<>();
    public static Integer gotoLimit = 20;
    public static boolean disableMockExploration = false;
    public static boolean requireMethodSuggestion = false;
    public static boolean skipGettersAndSetters = true;

    public static boolean shouldMockOrPropagate(String pckg) {
        return shouldPropagate(pckg) || shouldMock(pckg);
    }

    public static boolean shouldPropagate(String pckg) {
        pckg = parseWin(pckg);
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
        pckg = parseWin(pckg);
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
            case TARGET_CLASS -> targetClass = parseWin(value);
            case WHITELISTED_CLASSES -> {
                for (String v : split(value))
                    whitelistedClasses.add(parseWin(v.strip()));
            }
            case WHITELISTED_PACKAGES -> {
                for (String v : split(value))
                    whitelistedPackages.add(parseWin(v.strip()));
            }
            case MOCKABLE_CLASSES -> {
                for (String v : split(value))
                    mockableClasses.add(parseWin(v.strip()));
            }
            case MOCKABLE_PACKAGES -> {
                for (String v : split(value))
                    mockablePackages.add(parseWin(v.strip()));
            }
            case RECURSION_LIMIT -> gotoLimit = Integer.parseInt(value);
            case LOG_LEVEL -> Logger.setLoggingLevel(Logger.Level.fromValue(value));
            case DISABLE_MOCK_EXPLORATION -> disableMockExploration = Boolean.parseBoolean(value);
            case REQUIRE_METHOD_SUGGESTION -> requireMethodSuggestion = Boolean.parseBoolean(value);
            case SKIP_GETTERS_AND_SETTERS -> skipGettersAndSetters = Boolean.parseBoolean(value);
        }
    }

    public static String parseWin(String value) {
        return value.replace("/", ".");
    }

    private static String[] split(String value) {
        return value.strip()
                .replace("\n","")
                .replace("\r","")
                .split(",");
    }
}
