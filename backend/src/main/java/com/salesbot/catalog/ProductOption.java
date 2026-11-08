package com.salesbot.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * A choice the customer makes for a product, e.g. "المقاس" with 40/41/42, or "السعة" with 128GB/256GB.
 * A value may carry its own price (only one option of a product may do that) and its own stock flag.
 */
public record ProductOption(String name, List<OptionValue> values) {

    public ProductOption {
        values = values == null ? List.of() : List.copyOf(values);
    }

    /** @param price null = the product's base price */
    public record OptionValue(String value, BigDecimal price, boolean inStock) {
    }

    /** A free "name: value" fact, e.g. "الماركة: سامسونج", "الضمان: سنة". */
    public record ProductSpec(String name, String value) {
    }

    /** What the customer picked for one option, e.g. "السعة" = "256GB". */
    public record OptionChoice(String name, String value) {
    }
}
