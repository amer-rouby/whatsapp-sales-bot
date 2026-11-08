package com.salesbot.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything a provider needs to answer.
 *
 * @param customerName  may be null
 * @param customerText  the new message
 * @param history       earlier turns, oldest first
 * @param products      the ONLY products the bot may talk about; empty means the store has none yet
 */
public record ReplyRequest(String customerName, String customerText, List<Turn> history, List<ProductFact> products) {

    public ReplyRequest {
        history = history == null ? List.of() : List.copyOf(history);
        products = products == null ? List.of() : List.copyOf(products);
    }

    public record Turn(Role role, String text) {
    }

    public enum Role { CUSTOMER, BOT }

    /**
     * A product as the AI sees it. Owned by this module so {@code ai} never depends on {@code catalog}.
     *
     * @param price     base price; an option value may have its own
     * @param hasPhoto  the bot may offer to send its photo
     */
    public record ProductFact(String name, String category, String description, BigDecimal price, String currency,
                              boolean inStock, List<Option> options, List<Spec> specs, boolean hasPhoto) {

        public ProductFact {
            options = options == null ? List.of() : List.copyOf(options);
            specs = specs == null ? List.of() : List.copyOf(specs);
        }
    }

    public record Option(String name, List<OptionValue> values) {

        public Option {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    /** @param price null = the product's base price */
    public record OptionValue(String value, BigDecimal price, boolean inStock) {
    }

    public record Spec(String name, String value) {
    }
}
