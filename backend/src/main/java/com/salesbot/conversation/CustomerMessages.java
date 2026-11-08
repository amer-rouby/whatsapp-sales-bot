package com.salesbot.conversation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public read API: what customers wrote recently (used by notifications). */
public interface CustomerMessages {

    /** Customer messages stored after {@code after}, newest first, at most {@code limit}. */
    List<CustomerMessage> since(Instant after, int limit);

    /**
     * @param simulated  sent from the dashboard simulator, not a real customer
     * @param botPaused  the shop took this chat over, so nobody answers unless a person does
     */
    record CustomerMessage(UUID messageId, UUID conversationId, String customerName, String customerWaId,
                           boolean simulated, boolean botPaused, String body, Instant at) {
    }
}
