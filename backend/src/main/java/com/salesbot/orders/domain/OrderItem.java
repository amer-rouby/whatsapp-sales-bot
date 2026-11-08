package com.salesbot.orders.domain;

import com.salesbot.orders.PlaceOrder;
import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_name", nullable = false)
    private String productName;

    /** What the customer picked, in order, e.g. السعة=256GB, اللون=أسود. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", nullable = false)
    private List<PlaceOrder.OrderLineChoice> options = new ArrayList<>();

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Catalog price at order time (of the chosen option when it has one); null when the product was not found. */
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    protected OrderItem() {
    }

    OrderItem(Order order, String productName, List<PlaceOrder.OrderLineChoice> options, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.productName = productName;
        this.options = new ArrayList<>(options == null ? List.of() : options);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getProductName() {
        return productName;
    }

    public List<PlaceOrder.OrderLineChoice> getOptions() {
        return List.copyOf(options);
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
