package com.salesbot.catalog.domain;

import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A product photo's details. The bytes live in {@link ProductImageFile} so listing products never loads them.
 * A photo is never edited in place: an edit uploads a new one, so its id can be cached forever.
 */
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "bytes", nullable = false)
    private int bytes;

    protected ProductImage() {
    }

    ProductImage(Product product, int position, Content content) {
        this.product = product;
        this.position = position;
        this.width = content.width();
        this.height = content.height();
        this.bytes = content.bytes();
    }

    void moveTo(int newPosition) {
        this.position = newPosition;
    }

    public Product getProduct() {
        return product;
    }

    public int getPosition() {
        return position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBytes() {
        return bytes;
    }

    /** Size facts of a processed photo. */
    public record Content(int width, int height, int bytes) {
    }
}
