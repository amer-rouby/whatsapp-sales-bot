package com.salesbot.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * The JSON shape Gemini must answer with (enforced by {@link #SCHEMA}), and its Java mirror.
 * One call returns both the chat text and, when the customer confirms, the order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record StructuredReply(String reply, Order order) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Order(String customerName, String address, List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String productName, String size, String color, Integer quantity) {
    }

    /** JSON Schema sent as {@code generationConfig.responseJsonSchema} (verified against the live API, Nov 2026). */
    static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "reply", Map.of(
                            "type", "string",
                            "description", "The WhatsApp message to send to the customer."),
                    "order", Map.of(
                            "description", "Only when the customer explicitly confirmed the order summary in THIS"
                                    + " message; otherwise null.",
                            "anyOf", List.of(
                                    Map.of("type", "null"),
                                    Map.of(
                                            "type", "object",
                                            "properties", Map.of(
                                                    "customerName", Map.of("type", "string"),
                                                    "address", Map.of("type", "string",
                                                            "description", "Governorate + full address"),
                                                    "items", Map.of(
                                                            "type", "array",
                                                            "items", Map.of(
                                                                    "type", "object",
                                                                    "properties", Map.of(
                                                                            "productName", Map.of("type", "string",
                                                                                    "description", "Exactly as in the product list"),
                                                                            "size", Map.of("type", "string"),
                                                                            "color", Map.of("type", "string"),
                                                                            "quantity", Map.of("type", "integer")),
                                                                    "required", List.of("productName", "quantity")))),
                                            "required", List.of("customerName", "address", "items"))))),
            "required", List.of("reply", "order"));
}
