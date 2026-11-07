package com.salesbot.ai;

import java.util.List;

/**
 * What the bot says, plus the order it closed in this message (if any).
 *
 * @param text  message sent to the customer
 * @param order non-null only in the message where the customer explicitly confirmed an order
 */
public record AiReply(String text, OrderDraft order) {

    public static AiReply text(String text) {
        return new AiReply(text, null);
    }

    /** An order as understood by the AI. Prices are NOT trusted from here — the orders module looks them up. */
    public record OrderDraft(String customerName, String address, List<Item> items) {

        public OrderDraft {
            items = items == null ? List.of() : List.copyOf(items);
        }

        public record Item(String productName, String size, String color, int quantity) {
        }
    }
}
