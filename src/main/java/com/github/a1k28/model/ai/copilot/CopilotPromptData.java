package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CopilotPromptData {
    @SerializedName("id")
    private String id;
    @SerializedName("created")
    private Long created;
    @SerializedName("choices")
    private List<CopilotPromptDataChoice> choices;
}