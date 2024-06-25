package com.github.a1k28.evoc.model.ai.claude.input;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TextMessage implements ClaudeMessage {
    private String content;

    @Override
    public JsonObject toJsonObject() {
        JsonObject object = new JsonObject();
        object.addProperty("type", "text");
        object.addProperty("text", content);
        return object;
    }
}
