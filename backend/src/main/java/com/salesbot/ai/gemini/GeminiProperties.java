package com.salesbot.ai.gemini;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code salesbot.ai.gemini.*}.
 *
 * @param models tried in order; the next one is used when a model is out of quota (429) or down (5xx).
 *               Free-tier quotas are per model, so a chain multiplies the free daily messages.
 */
@ConfigurationProperties("salesbot.ai.gemini")
public record GeminiProperties(
        String apiKey,
        List<String> models,
        String baseUrl,
        Duration timeout,
        double temperature,
        String thinkingLevel,
        String systemPrompt) {

    public GeminiProperties {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("salesbot.ai.gemini.models must list at least one model");
        }
        models = List.copyOf(models);
    }
}
