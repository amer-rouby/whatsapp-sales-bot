package com.salesbot.orders.api;

import com.salesbot.orders.application.OrderService;
import com.salesbot.orders.domain.Order;
import com.salesbot.orders.domain.OrderItem;
import com.salesbot.orders.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard API: the orders the bot closed, and their fulfilment status. */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    List<OrderResponse> list() {
        return service.list().stream().map(OrderResponse::of).toList();
    }

    @PatchMapping("/{id}/status")
    OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange change) {
        return OrderResponse.of(service.changeStatus(id, change.status()));
    }

    record StatusChange(@NotNull OrderStatus status) {
    }

    record OrderResponse(UUID id, String customerName, String customerPhone, String shippingAddress,
                         OrderStatus status, BigDecimal total, String currency, boolean needsReview,
                         List<OrderLineResponse> items, Instant createdAt) {

        static OrderResponse of(Order o) {
            return new OrderResponse(o.getId(), o.getCustomerName(), o.getCustomerPhone(), o.getShippingAddress(),
                    o.getStatus(), o.getTotal(), o.getCurrency(), o.isNeedsReview(),
                    o.getItems().stream().map(OrderLineResponse::of).toList(), o.getCreatedAt());
        }
    }

    /** Named explicitly: the class name becomes the OpenAPI schema name the dashboard types use. */
    record OrderLineResponse(String productName, List<OrderChoice> options, int quantity, BigDecimal unitPrice) {

        static OrderLineResponse of(OrderItem i) {
            return new OrderLineResponse(i.getProductName(),
                    i.getOptions().stream().map(c -> new OrderChoice(c.name(), c.value())).toList(),
                    i.getQuantity(), i.getUnitPrice());
        }
    }

    /** One choice on an order line, e.g. السعة = 256GB. */
    record OrderChoice(String name, String value) {
    }
}
