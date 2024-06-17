package com.github.a1k28.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopilotVerificationModel {
    @SerializedName("device_code")
    private String deviceCode;
    @SerializedName("user_code")
    private String userCode;
    @SerializedName("verification_uri")
    private String verificationUri;
}
