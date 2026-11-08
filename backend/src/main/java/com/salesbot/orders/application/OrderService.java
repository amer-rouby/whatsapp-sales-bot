package com.salesbot.orders.application;

import com.salesbot.catalog.CatalogQuery;
import com.salesbot.catalog.ProductOption;
import com.salesbot.orders.OrderPlacement;
import com.salesbot.orders.PlaceOrder;
import com.salesbot.orders.RecentOrders;
import com.salesbot.orders.domain.Order;
import com.salesbot.orders.domain.OrderStatus;
import com.salesbot.orders.infrastructure.OrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService implements OrderPlacement, RecentOrders {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    /** A second confirmation of the same items within this window is the same order, not a new one. */
    static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(15);

    private final OrderRepository orders;
    private final CatalogQuery catalog;

    OrderService(OrderRepository orders, CatalogQuery catalog) {
        this.orders = orders;
        this.catalog = catalog;
    }

    @Override
    public Optional<UUID> place(PlaceOrder command) {
        if (command.items().isEmpty()) {
            return Optional.empty();
        }
        Order order = new Order(command.conversationId(), command.customerPhone(), command.customerName(),
                command.address());
        for (PlaceOrder.Item item : command.items()) {
            // the price comes from the catalog (with the chosen option's price when it has one), never from the AI
            Optional<CatalogQuery.Quote> quote = catalog.quote(item.productName(), item.options().stream()
                    .map(c -> new ProductOption.OptionChoice(c.name(), c.value()))
                    .toList());
            order.addItem(
                    quote.map(CatalogQuery.Quote::productName).orElse(item.productName()),
                    item.options(), item.quantity(),
                    quote.map(CatalogQuery.Quote::unitPrice).orElse(null),
                    quote.map(CatalogQuery.Quote::currency).orElse(null),
                    quote.map(CatalogQuery.Quote::valid).orElse(false));
        }

        if (isDuplicate(order)) {
            log.info("Same order already recorded for conversation {}, skipped", command.conversationId());
            return Optional.empty();
        }
        Order saved = orders.save(order);
        log.info("Order {} recorded: total={} {} needsReview={}", saved.getId(), saved.getTotal(),
                saved.getCurrency(), saved.isNeedsReview());
        return Optional.of(saved.getId());
    }

    private boolean isDuplicate(Order candidate) {
        if (candidate.getConversationId() == null) {
            return false;
        }
        String fingerprint = candidate.itemsFingerprint();
        return orders.findByConversationIdAndStatusAndCreatedAtAfter(candidate.getConversationId(),
                        OrderStatus.NEW, Instant.now().minus(DUPLICATE_WINDOW)).stream()
                .anyMatch(existing -> existing.itemsFingerprint().equals(fingerprint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderNotice> since(Instant after, int limit) {
        return orders.findByCreatedAtAfterOrderByCreatedAtDesc(after, Limit.of(limit)).stream()
                .map(o -> new OrderNotice(o.getId(), o.getConversationId(), o.getCustomerName(), o.getCustomerPhone(),
                        o.getTotal(), o.getCurrency(), o.isNeedsReview(), o.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Order> list() {
        return orders.findAllByOrderByCreatedAtDesc();
    }

    public Order changeStatus(UUID id, OrderStatus status) {
        Order order = orders.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
        order.changeStatus(status);
        return order;
    }
}
