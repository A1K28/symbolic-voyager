package com.github.a1k28.evoc.model.ai.claude.input;

import com.google.gson.JsonObject;

public interface ClaudeMessage {
    JsonObject toJsonObject();
}
