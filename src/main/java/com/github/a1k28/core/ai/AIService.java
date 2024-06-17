package com.github.a1k28.core.ai;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AIService {
    String generate(String prompt, String language) throws URISyntaxException, IOException, InterruptedException;
}
