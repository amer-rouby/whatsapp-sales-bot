package com.salesbot.catalog.infrastructure;

import com.salesbot.catalog.domain.ProductImageFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageFileRepository extends JpaRepository<ProductImageFile, UUID> {
}
