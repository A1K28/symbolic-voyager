package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopilotToken {
    @SerializedName("token")
    private String token;
}
