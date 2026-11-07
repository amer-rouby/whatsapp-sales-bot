package com.salesbot.catalog.application;

import com.salesbot.catalog.CatalogQuery;
import com.salesbot.catalog.ProductSummary;
import com.salesbot.catalog.domain.Product;
import com.salesbot.catalog.infrastructure.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService implements CatalogQuery {

    private final ProductRepository products;

    ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        return products.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Product get(UUID id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(Product.Details details) {
        return products.save(new Product(details));
    }

    public Product update(UUID id, Product.Details details) {
        Product product = get(id);
        product.apply(details);
        return product;
    }

    public void delete(UUID id) {
        products.delete(get(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummary> products() {
        return products.findAllByOrderByCreatedAtDesc(Limit.of(MAX_PRODUCTS)).stream()
                .map(ProductService::summary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummary> findByName(String name) {
        String cleaned = cleanName(name);
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        return products.findFirstByNameIgnoreCase(cleaned)
                .or(() -> products.findFirstByNameIgnoreCase(withoutTrailingNote(cleaned)))
                .map(ProductService::summary);
    }

    /** Drops «» quotes and repeated spaces the AI may copy from the prompt. */
    static String cleanName(String name) {
        return name == null ? "" : name.replace("«", "").replace("»", "").replaceAll("\\s+", " ").strip();
    }

    /** "كوتشي أبيض (أحذية)" → "كوتشي أبيض": the AI sometimes appends the category or a note. */
    static String withoutTrailingNote(String name) {
        return name.replaceAll("\\s*[(\\[][^()\\[\\]]*[)\\]]\\s*$", "").strip();
    }

    private static ProductSummary summary(Product p) {
        return new ProductSummary(p.getName(), p.getCategory(), p.getDescription(), p.getPrice(),
                p.getCurrency(), p.getSizes(), p.getColors(), p.isInStock());
    }
}
