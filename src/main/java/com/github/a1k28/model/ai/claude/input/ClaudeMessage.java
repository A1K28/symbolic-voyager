package com.github.a1k28.model.ai.claude.input;

import com.google.gson.JsonObject;

public interface ClaudeMessage {
    JsonObject toJsonObject();
}
