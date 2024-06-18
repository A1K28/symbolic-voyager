package com.github.a1k28.model.ai.claude;

import com.github.a1k28.model.ai.claude.input.RoleInput;
import com.github.a1k28.model.ai.claude.types.ClaudeModel;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ClaudeRequest {
    @NonNull private final ClaudeModel model;
    @Builder.Default private final int max_tokens = 1000;
    private String system;
    @Singular private List<String> stop_sequences;
    @Builder.Default private double temperature = 0.0;
    @NonNull @Singular private final List<RoleInput> inputs;
}
