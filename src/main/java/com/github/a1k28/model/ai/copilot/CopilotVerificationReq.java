package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CopilotVerificationReq {
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("scope")
    private String scope;
}
