package com.salesbot.catalog.application;

import com.salesbot.catalog.CatalogQuery;
import com.salesbot.catalog.ProductOption;
import com.salesbot.catalog.ProductSummary;
import com.salesbot.catalog.domain.Product;
import com.salesbot.catalog.domain.ProductImage;
import com.salesbot.catalog.domain.ProductImageFile;
import com.salesbot.catalog.infrastructure.ProductImageFileRepository;
import com.salesbot.catalog.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductService implements CatalogQuery {

    private final ProductRepository products;
    private final ProductImageFileRepository files;
    private final ImageProcessor processor;

    ProductService(ProductRepository products, ProductImageFileRepository files,
                   ImageProcessor processor) {
        this.products = products;
        this.files = files;
        this.processor = processor;
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        List<Product> all = products.findAllByOrderByCreatedAtDesc();
        all.forEach(p -> p.getImages().size()); // load photo details (batched) inside the transaction
        return all;
    }

    @Transactional(readOnly = true)
    public Product get(UUID id) {
        Product product = find(id);
        product.getImages().size();
        return product;
    }

    public Product create(Product.Details details) {
        return products.save(new Product(details));
    }

    public Product update(UUID id, Product.Details details) {
        Product product = find(id);
        product.apply(details);
        product.getImages().size();
        return product;
    }

    public void delete(UUID id) {
        products.delete(find(id));
    }

    // ---- photos -------------------------------------------------------------------------------

    /** Validates and re-encodes the upload, then appends it to the gallery (the first photo is the main one). */
    public ProductImage addImage(UUID productId, byte[] upload) {
        ImageProcessor.Processed processed = processor.process(upload);
        Product product = find(productId);
        ProductImage image = product.addImage(
                new ProductImage.Content(processed.width(), processed.height(), processed.full().length));
        products.flush(); // the product is managed: flushing persists the new photo itself and gives it its id
        files.save(new ProductImageFile(image.getId(), processed.full(), processed.thumb()));
        return image;
    }

    public void removeImage(UUID productId, UUID imageId) {
        if (!find(productId).removeImage(imageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo " + imageId + " not found");
        }
    }

    public Product reorderImages(UUID productId, List<UUID> order) {
        Product product = find(productId);
        product.reorderImages(order);
        return product;
    }

    /** The stored JPEG (full size or thumbnail). */
    @Transactional(readOnly = true)
    public byte[] imageBytes(UUID imageId, boolean thumbnail) {
        ProductImageFile file = files.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo " + imageId + " not found"));
        return thumbnail ? file.getThumb() : file.getData();
    }

    // ---- what the bot and the orders module read ---------------------------------------------

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
        return findProductByName(name).map(ProductService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quote> quote(String productName, List<ProductOption.OptionChoice> choices) {
        return findProductByName(productName).map(product -> priced(product, choices));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> mainPhoto(String productName) {
        return findProductByName(productName).flatMap(product -> product.getImages().stream().findFirst()
                .flatMap(image -> files.findById(image.getId()))
                .map(file -> new Photo(file.getImageId(), product.getName(), "image/jpeg", file.getData())));
    }

    /**
     * Unit price = the price of the chosen value in the option that sets prices, else the base price.
     * Invalid when the product or a chosen value is sold out, a value doesn't exist, or the priced option wasn't chosen.
     */
    static Quote priced(Product product, List<ProductOption.OptionChoice> choices) {
        BigDecimal unitPrice = product.getPrice();
        boolean valid = product.isInStock();
        List<ProductOption.OptionChoice> picked = choices == null ? List.of() : choices;
        for (ProductOption option : product.getOptions()) {
            Optional<ProductOption.OptionValue> chosen = picked.stream()
                    .filter(c -> c != null && same(c.name(), option.name()))
                    .findFirst()
                    .flatMap(c -> option.values().stream().filter(v -> same(v.value(), c.value())).findFirst());
            boolean optionSetsPrice = option.values().stream().anyMatch(v -> v.price() != null);
            if (chosen.isEmpty()) {
                // unknown value, or a price-setting option nobody picked: a person must check
                boolean namedButUnknown = picked.stream().anyMatch(c -> c != null && same(c.name(), option.name()));
                if (namedButUnknown || optionSetsPrice) {
                    valid = false;
                }
                continue;
            }
            if (!chosen.get().inStock()) {
                valid = false;
            }
            if (chosen.get().price() != null) {
                unitPrice = chosen.get().price();
            }
        }
        // a choice for an option the product doesn't have
        for (ProductOption.OptionChoice choice : picked) {
            if (choice != null && product.getOptions().stream().noneMatch(o -> same(o.name(), choice.name()))) {
                valid = false;
            }
        }
        return new Quote(product.getName(), unitPrice, product.getCurrency(), valid);
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && normalize(a).equals(normalize(b));
    }

    private static String normalize(String s) {
        return s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Optional<Product> findProductByName(String name) {
        String cleaned = cleanName(name);
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        return products.findFirstByNameIgnoreCase(cleaned)
                .or(() -> products.findFirstByNameIgnoreCase(withoutTrailingNote(cleaned)));
    }

    private Product find(UUID id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
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
        return new ProductSummary(p.getName(), p.getCategory(), p.getDescription(), p.getPrice(), p.getCurrency(),
                p.isInStock(), p.getOptions(), p.getSpecs(), p.getImages().size());
    }
}
