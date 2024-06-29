package com.github.a1k28.evoc.model.ai.claude.types;

import lombok.Getter;

@Getter
public enum ClaudeModel {
    SONNET("claude-3-sonnet-20240229");

    private final String id;

    ClaudeModel(String id) {
        this.id = id;
    }

    public static ClaudeModel fromId(String id) {
        for (ClaudeModel model : values()) {
            if (model.id.equals(id)) {
                return model;
            }
        }
        throw new IllegalArgumentException("Unknown model id: " + id);
    }

}
