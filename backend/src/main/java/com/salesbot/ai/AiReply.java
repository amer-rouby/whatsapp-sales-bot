package com.salesbot.ai;

import java.util.List;

/**
 * What the bot says, the products whose photos go with it, and the order it closed in this message (if any).
 *
 * @param text   message sent to the customer
 * @param photos product names whose main photo should be sent after the text (at most {@link #MAX_PHOTOS})
 * @param order  non-null only in the message where the customer explicitly confirmed an order
 */
public record AiReply(String text, List<String> photos, OrderDraft order) {

    public static final int MAX_PHOTOS = 3;

    public AiReply {
        photos = photos == null ? List.of() : photos.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::strip)
                .distinct()
                .limit(MAX_PHOTOS)
                .toList();
    }

    public static AiReply text(String text) {
        return new AiReply(text, List.of(), null);
    }

    /** An order as understood by the AI. Prices are NOT trusted from here — the orders module looks them up. */
    public record OrderDraft(String customerName, String address, List<Item> items) {

        public OrderDraft {
            items = items == null ? List.of() : List.copyOf(items);
        }

        /** @param options what the customer picked, in order, e.g. السعة=256GB */
        public record Item(String productName, List<Choice> options, int quantity) {

            public Item {
                options = options == null ? List.of() : List.copyOf(options);
            }
        }

        public record Choice(String name, String value) {
        }
    }
}
