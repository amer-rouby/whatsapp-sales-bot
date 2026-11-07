package com.salesbot.catalog;

import java.util.List;
import java.util.Optional;

/** Public read API of the catalog — what the bot is allowed to sell. */
public interface CatalogQuery {

    /** Max products handed to the bot in one conversation turn. */
    int MAX_PRODUCTS = 100;

    /** Every product (in stock and sold out), newest first, capped at {@link #MAX_PRODUCTS}. */
    List<ProductSummary> products();

    /** Exact name match, ignoring case and surrounding spaces. */
    Optional<ProductSummary> findByName(String name);
}
