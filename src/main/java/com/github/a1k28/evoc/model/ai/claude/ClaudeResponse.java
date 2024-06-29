package com.github.a1k28.evoc.model.ai.claude;

import com.github.a1k28.evoc.model.ai.claude.types.ClaudeRole;
import com.github.a1k28.evoc.model.ai.claude.types.ClaudeStopReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class ClaudeResponse {
    private String id;
    private String type;
    private ClaudeRole role;
    private List<String> content;
    private String model;
    private ClaudeStopReason stop_reason;
    private String stop_sequence;
    private ClaudeUsage usage;
}
