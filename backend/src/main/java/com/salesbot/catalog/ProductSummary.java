package com.salesbot.catalog;

import java.math.BigDecimal;

/** What other modules may know about a product (read-only view). */
public record ProductSummary(
        String name,
        String category,
        String description,
        BigDecimal price,
        String currency,
        String sizes,
        String colors,
        boolean inStock) {
}
