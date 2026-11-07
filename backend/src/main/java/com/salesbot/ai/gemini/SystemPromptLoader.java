package com.salesbot.ai.gemini;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Reads the bot's instructions from a file, so wording changes need no code change. */
@Component
class SystemPromptLoader {

    private final ResourceLoader resourceLoader;

    SystemPromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** @param location e.g. {@code classpath:prompts/sales-assistant-ar.txt} or {@code file:...} */
    String load(String location) {
        try (InputStream in = resourceLoader.getResource(location).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read system prompt from " + location, e);
        }
    }
}
