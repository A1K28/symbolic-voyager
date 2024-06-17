package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopilotPromptExtra {
    @SerializedName("language")
    private String language;
}
