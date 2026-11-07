package com.salesbot.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Request/response shapes of the Gemini {@code generateContent} REST call (only the fields we use). */
final class GeminiApi {

    private GeminiApi() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {

        static Content of(String role, String text) {
            return new Content(role, List.of(new Part(text)));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(double temperature, ThinkingConfig thinkingConfig,
                            String responseMimeType, Object responseJsonSchema) {
    }

    /** Gemini 3+: minimal | low | medium | high. Lower = faster replies. */
    record ThinkingConfig(String thinkingLevel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }
}
