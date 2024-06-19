package com.github.a1k28.model.ai.claude.types;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ImageType {
    JPEG("jpeg"),
    PNG("png");

    private final String value;

    public String value() {
        return this.value;
    }
}
