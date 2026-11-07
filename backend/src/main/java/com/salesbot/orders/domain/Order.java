package com.salesbot.orders.domain;

import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entity name avoids ORDER, a reserved word in JPQL. */
@Entity(name = "SalesOrder")
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "currency")
    private String currency;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID conversationId, String customerPhone, String customerName, String shippingAddress) {
        this.conversationId = conversationId;
        this.customerPhone = customerPhone;
        this.customerName = customerName;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.NEW;
    }

    /** @param unitPrice null when the product is unknown → the order is flagged for review */
    public void addItem(String productName, String size, String color, int quantity, BigDecimal unitPrice,
                        String priceCurrency) {
        items.add(new OrderItem(this, productName, size, color, quantity, unitPrice));
        if (unitPrice == null) {
            needsReview = true;
        } else if (currency == null) {
            currency = priceCurrency;
        } else if (!currency.equals(priceCurrency)) {
            needsReview = true; // mixed currencies can't be summed honestly
        }
        recalculateTotal();
    }

    public void changeStatus(OrderStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
    }

    /** Sum of the known lines; the review flag tells the owner when it's incomplete. */
    private void recalculateTotal() {
        total = items.stream()
                .map(OrderItem::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Stable fingerprint of the items, used to spot the same order being recorded twice. */
    public String itemsFingerprint() {
        return items.stream()
                .map(i -> String.join("|", i.getProductName().toLowerCase(), String.valueOf(i.getSize()),
                        String.valueOf(i.getColor()), String.valueOf(i.getQuantity())))
                .sorted()
                .reduce("", (a, b) -> a + ";" + b);
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
