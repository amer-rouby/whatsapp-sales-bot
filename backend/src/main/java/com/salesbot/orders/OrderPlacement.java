package com.salesbot.orders;

import java.util.Optional;
import java.util.UUID;

/** Public write API of the orders module. */
public interface OrderPlacement {

    /**
     * Records the order with catalog prices.
     *
     * @return the new order id, or empty when the same order was already recorded moments ago
     */
    Optional<UUID> place(PlaceOrder command);
}
