package com.github.a1k28.symvoyager.core.cli.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CommandFlag {
    TARGET_CLASS("tc", "target-class", true, "The target class, against which the test methods will be generated."),
    WHITELISTED_CLASSES("wc", "whitelisted-classes", true, "Whitelisted classes that are allowed to be propagated (comma separated list)."),
    WHITELISTED_PACKAGES("wp", "whitelisted-packages", true, "Whitelisted packages that are allowed to be propagated (comma separated list)."),
    MOCKABLE_CLASSES("mc", "mockable-classes", true, "Classes that should be mocked (comma separated list)."),
    MOCKABLE_PACKAGES("mp", "mockable-packages", true, "Packages that should be mocked (comma separated list)."),
    DEPTH_LIMIT("dl", "depth-limit", true, "Exploration Depth limit. Default value is 100."),
    LOG_LEVEL("ll", "log-level", true, "Possible values: INFO, DEBUG, TRACE"),
    DISABLE_MOCK_EXPLORATION("dme", "disable-mock-exploration", true, "Possible values: true, false (default)"),
    REQUIRE_METHOD_SUGGESTION("rms", "require-method-suggestion", true, "Possible values: true, false (default)"),
    SKIP_GETTERS_AND_SETTERS("sgs", "skip-getters-setters", true, "Possible values: true (default), false");

    private final String option;
    private final String longOption;
    private final boolean hasArg;
    private final String description;

    public String option() {
        return this.option;
    }

    public String longOption() {
        return this.longOption;
    }

    public boolean hasArg() {
        return this.hasArg;
    }

    public String description() {
        return this.description;
    }
}
