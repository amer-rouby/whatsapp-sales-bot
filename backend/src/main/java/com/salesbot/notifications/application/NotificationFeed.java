package com.salesbot.notifications.application;

import com.salesbot.conversation.CustomerMessages;
import com.salesbot.orders.RecentOrders;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Merges customer messages and orders into one newest-first feed. The dashboard polls it with the
 * {@code serverTime} of its previous call, so the server's clock decides what is new (no client clock skew).
 * Item ids are stable, so the client can merge overlapping results.
 */
@Service
public class NotificationFeed {

    /** What the bell shows on first load. */
    static final Duration FIRST_LOAD_WINDOW = Duration.ofDays(2);
    /**
     * Re-read a little before the previous poll: a message stamped just before it may have been committed
     * just after it. The dashboard drops ids it already has, so the overlap never shows twice.
     */
    static final Duration OVERLAP = Duration.ofSeconds(30);
    static final int MAX_ITEMS = 50;
    /** Long chats are cut in the bell; the full text is in the conversation. */
    static final int PREVIEW_LENGTH = 140;

    private final CustomerMessages customerMessages;
    private final RecentOrders recentOrders;

    NotificationFeed(CustomerMessages customerMessages, RecentOrders recentOrders) {
        this.customerMessages = customerMessages;
        this.recentOrders = recentOrders;
    }

    /** @param after the {@code serverTime} of the previous call; null on first load */
    public NotificationPage since(Instant after) {
        // read the clock first: anything stored while we query lands in the next poll, never lost
        Instant now = Instant.now();
        Instant from = after != null ? after.minus(OVERLAP) : now.minus(FIRST_LOAD_WINDOW);

        List<NotificationItem> items = new ArrayList<>();
        customerMessages.since(from, MAX_ITEMS).forEach(m -> items.add(new NotificationItem(
                "message-" + m.messageId(), Type.CUSTOMER_MESSAGE, m.conversationId(), null,
                m.customerName(), m.customerWaId(), preview(m.body()), null, null,
                m.botPaused(), m.simulated(), m.at())));
        recentOrders.since(from, MAX_ITEMS).forEach(o -> items.add(new NotificationItem(
                "order-" + o.id(), Type.NEW_ORDER, o.conversationId(), o.id(),
                o.customerName(), o.customerPhone(), null, o.total(), o.currency(),
                o.needsReview(), false, o.at())));

        items.sort(Comparator.comparing(NotificationItem::at).reversed());
        return new NotificationPage(now, items.size() > MAX_ITEMS ? items.subList(0, MAX_ITEMS) : items);
    }

    private static String preview(String body) {
        return body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH) + "…";
    }

    public enum Type { CUSTOMER_MESSAGE, NEW_ORDER }

    /**
     * @param needsAttention a person must act: the bot is paused in that chat, or the order needs review
     * @param simulated      from the dashboard simulator (the bell stays quiet for those)
     */
    public record NotificationItem(String id, Type type, UUID conversationId, UUID orderId, String customerName,
                               String customerContact, String preview, BigDecimal total, String currency,
                               boolean needsAttention, boolean simulated, Instant at) {
    }

    /** @param serverTime pass it back as {@code after} on the next call */
    public record NotificationPage(Instant serverTime, List<NotificationItem> items) {
    }
}
