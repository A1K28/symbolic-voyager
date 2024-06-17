package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopilotPromptDataChoice {
    @SerializedName("text")
    private String text;
    @SerializedName("index")
    private Long index;
    @SerializedName("finish_reason")
    private String finishReason;
}