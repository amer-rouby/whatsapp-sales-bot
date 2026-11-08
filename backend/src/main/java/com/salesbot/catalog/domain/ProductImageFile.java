package com.salesbot.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** The JPEG bytes of a {@link ProductImage}: full size and a small thumbnail. Deleted with the photo (FK cascade). */
@Entity
@Table(name = "product_image_files")
public class ProductImageFile {

    @Id
    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "thumb", nullable = false)
    private byte[] thumb;

    protected ProductImageFile() {
    }

    public ProductImageFile(UUID imageId, byte[] data, byte[] thumb) {
        this.imageId = imageId;
        this.data = data;
        this.thumb = thumb;
    }

    public UUID getImageId() {
        return imageId;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getThumb() {
        return thumb;
    }
}
