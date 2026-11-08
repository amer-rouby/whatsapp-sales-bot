package com.salesbot.orders.infrastructure;

import com.salesbot.orders.domain.Order;
import com.salesbot.orders.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Newest first, items loaded in the same query (no N+1 on the orders page). */
    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    /** Items fetched eagerly: the API maps them after the transaction (open-in-view is off). */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    /** Header columns only (no items): enough for a notification. */
    List<Order> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after, Limit limit);

    List<Order> findByConversationIdAndStatusAndCreatedAtAfter(UUID conversationId, OrderStatus status,
                                                              Instant after);
}
