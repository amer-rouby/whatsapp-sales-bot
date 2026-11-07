package com.salesbot.catalog.domain;

import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "sizes")
    private String sizes;

    @Column(name = "colors")
    private String colors;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    protected Product() {
    }

    public Product(Details details) {
        apply(details);
    }

    /** Replaces every editable field (PUT semantics). */
    public void apply(Details details) {
        this.name = details.name().strip();
        this.category = blankToNull(details.category());
        this.description = blankToNull(details.description());
        this.price = details.price();
        this.currency = details.currency().toUpperCase();
        this.sizes = blankToNull(details.sizes());
        this.colors = blankToNull(details.colors());
        this.inStock = details.inStock();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSizes() {
        return sizes;
    }

    public String getColors() {
        return colors;
    }

    public boolean isInStock() {
        return inStock;
    }

    /** The editable fields, validated by the caller. */
    public record Details(String name, String category, String description, BigDecimal price, String currency,
                          String sizes, String colors, boolean inStock) {
    }
}
