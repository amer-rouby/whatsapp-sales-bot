package com.salesbot.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public read API of the catalog — what the bot is allowed to sell. */
public interface CatalogQuery {

    /** Max products handed to the bot in one conversation turn. */
    int MAX_PRODUCTS = 100;

    /** Every product (in stock and sold out), newest first, capped at {@link #MAX_PRODUCTS}. */
    List<ProductSummary> products();

    /** Exact name match, ignoring case, «» quotes, surrounding spaces and a trailing "(note)". */
    Optional<ProductSummary> findByName(String name);

    /**
     * The price of one unit with the customer's choices. Empty when the product doesn't exist.
     * {@code valid} is false when a choice is unknown or sold out, or the product itself is sold out.
     */
    Optional<Quote> quote(String productName, List<ProductOption.OptionChoice> choices);

    /** The product's main photo (first in its gallery), for sending to a customer. */
    Optional<Photo> mainPhoto(String productName);

    record Quote(String productName, BigDecimal unitPrice, String currency, boolean valid) {
    }

    /** A JPEG ready to send; {@code imageId} is stable for caching (a new edit gets a new id). */
    record Photo(UUID imageId, String productName, String contentType, byte[] data) {
    }
}
