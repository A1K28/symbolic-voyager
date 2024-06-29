package com.github.a1k28.evoc.model.ai.claude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class ClaudeUsage {
    private int input_tokens;
    private int output_tokens;
}
