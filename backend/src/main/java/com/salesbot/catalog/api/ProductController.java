package com.salesbot.catalog.api;

import com.salesbot.catalog.application.ProductService;
import com.salesbot.catalog.domain.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard API for managing the store's products. */
@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    List<ProductResponse> list() {
        return service.list().stream().map(ProductResponse::of).toList();
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.of(service.get(id));
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product product = service.create(request.toDetails());
        return ResponseEntity.created(URI.create("/api/products/" + product.getId()))
                .body(ProductResponse.of(product));
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.of(service.update(id, request.toDetails()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 100) String category,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code like EGP") String currency,
            @Size(max = 200) String sizes,
            @Size(max = 200) String colors,
            boolean inStock) {

        Product.Details toDetails() {
            return new Product.Details(name, category, description, price, currency, sizes, colors, inStock);
        }
    }

    record ProductResponse(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal price,
            String currency,
            String sizes,
            String colors,
            boolean inStock,
            Instant createdAt,
            Instant updatedAt) {

        static ProductResponse of(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getDescription(), p.getPrice(),
                    p.getCurrency(), p.getSizes(), p.getColors(), p.isInStock(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
