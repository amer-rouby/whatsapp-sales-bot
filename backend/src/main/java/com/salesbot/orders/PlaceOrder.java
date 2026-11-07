package com.salesbot.orders;

import java.util.List;
import java.util.UUID;

/**
 * Everything needed to record an order the customer confirmed in chat.
 *
 * @param conversationId the chat it came from (used to avoid recording the same order twice)
 * @param customerPhone  the customer's WhatsApp number
 */
public record PlaceOrder(UUID conversationId, String customerPhone, String customerName, String address,
                         List<Item> items) {

    public PlaceOrder {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(String productName, String size, String color, int quantity) {
    }
}
