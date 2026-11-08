package com.salesbot.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * The JSON shape Gemini must answer with (enforced by {@link #SCHEMA}), and its Java mirror.
 * One call returns the chat text, the product photos to send with it, and, when the customer confirms, the order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record StructuredReply(String reply, List<String> photos, Order order) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Order(String customerName, String address, List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String productName, List<Choice> options, Integer quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(String name, String value) {
    }

    /** JSON Schema sent as {@code generationConfig.responseJsonSchema}. Maps aren't allowed, so choices are a list. */
    static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "reply", Map.of(
                            "type", "string",
                            "description", "The WhatsApp message to send to the customer."),
                    "photos", Map.of(
                            "type", "array",
                            "description", "Names of products (exactly as in the product list, only ones that have"
                                    + " photos) whose photo is sent right after the reply. Usually empty; at most 3.",
                            "items", Map.of("type", "string")),
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
                                                                            "options", Map.of(
                                                                                    "type", "array",
                                                                                    "description", "One entry per option the customer picked,"
                                                                                            + " names and values exactly as in the product list",
                                                                                    "items", Map.of(
                                                                                            "type", "object",
                                                                                            "properties", Map.of(
                                                                                                    "name", Map.of("type", "string"),
                                                                                                    "value", Map.of("type", "string")),
                                                                                            "required", List.of("name", "value"))),
                                                                            "quantity", Map.of("type", "integer")),
                                                                    "required", List.of("productName", "quantity")))),
                                            "required", List.of("customerName", "address", "items"))))),
            "required", List.of("reply", "photos", "order"));
}
