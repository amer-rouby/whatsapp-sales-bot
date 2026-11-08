package com.salesbot.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * What other modules may know about a product (read-only view).
 *
 * @param price     base price; an option value may override it
 * @param imageCount how many photos the product has (the bot can send the main one)
 */
public record ProductSummary(
        String name,
        String category,
        String description,
        BigDecimal price,
        String currency,
        boolean inStock,
        List<ProductOption> options,
        List<ProductOption.ProductSpec> specs,
        int imageCount) {

    public ProductSummary {
        options = options == null ? List.of() : List.copyOf(options);
        specs = specs == null ? List.of() : List.copyOf(specs);
    }
}
