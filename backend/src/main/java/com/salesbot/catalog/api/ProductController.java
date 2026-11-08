package com.salesbot.catalog.api;

import com.salesbot.catalog.ProductOption;
import com.salesbot.catalog.application.ProductService;
import com.salesbot.catalog.domain.Product;
import com.salesbot.catalog.domain.ProductImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Dashboard API for managing the store's products and their photos. */
@RestController
@RequestMapping("/api/products")
class ProductController {

    /** A photo id never changes content (an edit uploads a new photo), so browsers may keep it for a year. */
    private static final CacheControl PHOTO_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable();

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

    // ---- photos -------------------------------------------------------------------------------

    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImageResponse> addImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) throws IOException {
        ProductImage image = service.addImage(id, file.getBytes());
        return ResponseEntity.created(URI.create("/api/products/images/" + image.getId())).body(ImageResponse.of(image));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    ResponseEntity<Void> removeImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        service.removeImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    /** The first id becomes the main photo (the one the bot sends). */
    @PutMapping("/{id}/images/order")
    ProductResponse reorderImages(@PathVariable UUID id, @Valid @RequestBody ImageOrderRequest request) {
        return ProductResponse.of(service.reorderImages(id, request.imageIds()));
    }

    @GetMapping(path = "/images/{imageId}", produces = MediaType.IMAGE_JPEG_VALUE)
    ResponseEntity<byte[]> image(@PathVariable UUID imageId, @RequestParam(defaultValue = "full") ImageSize size) {
        byte[] bytes = service.imageBytes(imageId, size == ImageSize.thumb);
        return ResponseEntity.ok()
                .cacheControl(PHOTO_CACHE)
                .eTag("\"" + imageId + "-" + size + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    enum ImageSize { full, thumb }

    // ---- request / response -----------------------------------------------------------------

    record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 100) String category,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code like EGP") String currency,
            @Size(max = Product.MAX_OPTIONS) List<@Valid OptionRequest> options,
            @Size(max = Product.MAX_SPECS) List<@Valid SpecRequest> specs,
            boolean inStock) {

        Product.Details toDetails() {
            List<ProductOption> opts = options == null ? List.of() : options.stream().map(OptionRequest::toOption).toList();
            List<ProductOption.ProductSpec> sp = specs == null ? List.of()
                    : specs.stream().map(s -> new ProductOption.ProductSpec(s.name(), s.value())).toList();
            return new Product.Details(name, category, description, price, currency, opts, sp, inStock);
        }
    }

    record OptionRequest(
            @Size(max = 100) String name,
            @NotNull @Size(max = Product.MAX_VALUES) List<@Valid ValueRequest> values) {

        ProductOption toOption() {
            return new ProductOption(name, values.stream()
                    .map(v -> new ProductOption.OptionValue(v.value(), v.price(), v.inStock() == null || v.inStock()))
                    .toList());
        }
    }

    /** @param inStock defaults to true when left out */
    record ValueRequest(
            @Size(max = 100) String value,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            Boolean inStock) {
    }

    record SpecRequest(@Size(max = 100) String name, @Size(max = 300) String value) {
    }

    record ImageOrderRequest(@NotNull @Size(max = Product.MAX_IMAGES) List<@NotNull UUID> imageIds) {
    }

    record ImageResponse(UUID id, int width, int height) {

        static ImageResponse of(ProductImage image) {
            return new ImageResponse(image.getId(), image.getWidth(), image.getHeight());
        }
    }

    record ProductResponse(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal price,
            String currency,
            boolean inStock,
            List<ProductOption> options,
            List<ProductOption.ProductSpec> specs,
            List<ImageResponse> images,
            Instant createdAt,
            Instant updatedAt) {

        static ProductResponse of(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getDescription(), p.getPrice(),
                    p.getCurrency(), p.isInStock(), p.getOptions(), p.getSpecs(),
                    p.getImages().stream().map(ImageResponse::of).toList(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
