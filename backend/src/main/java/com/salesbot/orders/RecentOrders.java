package com.salesbot.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public read API: orders recorded recently (used by notifications). */
public interface RecentOrders {

    /** Orders created after {@code after}, newest first, at most {@code limit}. */
    List<OrderNotice> since(Instant after, int limit);

    record OrderNotice(UUID id, UUID conversationId, String customerName, String customerPhone,
                       BigDecimal total, String currency, boolean needsReview, Instant at) {
    }
}
