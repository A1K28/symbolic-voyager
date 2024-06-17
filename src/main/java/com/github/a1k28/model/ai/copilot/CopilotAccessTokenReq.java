package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopilotAccessTokenReq {
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("device_code")
    private String deviceCode;
    @SerializedName("grant_type")
    private String grantType;
}
