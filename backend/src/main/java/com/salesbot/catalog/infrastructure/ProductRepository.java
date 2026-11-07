package com.salesbot.catalog.infrastructure;

import com.salesbot.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllByOrderByCreatedAtDesc(Limit limit);

    Optional<Product> findFirstByNameIgnoreCase(String name);
}
