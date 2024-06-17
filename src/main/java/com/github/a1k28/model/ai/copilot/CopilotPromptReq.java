package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CopilotPromptReq {
    @SerializedName("prompt")
    private String prompt;
    @SerializedName("suffix")
    private String suffix;
    @SerializedName("max_tokens")
    private Integer maxTokens;
    @SerializedName("temperature")
    private Integer temperature;
    @SerializedName("top_p")
    private Integer topP;
    @SerializedName("n")
    private Integer n;
    @SerializedName("stop")
    private List<String> stop;
    @SerializedName("nwo")
    private String nwo;
    @SerializedName("stream")
    private Boolean stream;
    @SerializedName("extra")
    private CopilotPromptExtra extra;
}