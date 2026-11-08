package com.salesbot.catalog.domain;

import com.salesbot.catalog.ProductOption;
import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    public static final int MAX_OPTIONS = 10;
    public static final int MAX_VALUES = 30;
    public static final int MAX_SPECS = 20;
    public static final int MAX_IMAGES = 8;
    private static final int MAX_TEXT = 100;

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

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", nullable = false)
    private List<ProductOption> options = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs", nullable = false)
    private List<ProductOption.ProductSpec> specs = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(Details details) {
        apply(details);
    }

    /** Replaces every editable field (PUT semantics). Photos are managed separately. */
    public void apply(Details details) {
        this.name = details.name().strip();
        this.category = blankToNull(details.category());
        this.description = blankToNull(details.description());
        this.price = details.price();
        this.currency = details.currency().toUpperCase(Locale.ROOT);
        this.options = cleanOptions(details.options());
        this.specs = cleanSpecs(details.specs());
        this.inStock = details.inStock();
    }

    /**
     * Trims names and values, drops rows left empty, and enforces what the bot and order pricing rely on:
     * unique option names, unique values inside an option, and prices set by one option at most.
     */
    static List<ProductOption> cleanOptions(List<ProductOption> raw) {
        List<ProductOption> cleaned = new ArrayList<>();
        Set<String> names = new HashSet<>();
        int pricedOptions = 0;
        for (ProductOption option : raw == null ? List.<ProductOption>of() : raw) {
            if (option == null) {
                continue;
            }
            String optionName = text(option.name(), "option name");
            List<ProductOption.OptionValue> values = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            boolean priced = false;
            for (ProductOption.OptionValue value : option.values()) {
                String v = value == null ? null : text(value.value(), "option value");
                if (v == null) {
                    continue;
                }
                if (!seen.add(v.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("\"" + v + "\" is repeated in option \"" + optionName + "\"");
                }
                if (value.price() != null && value.price().signum() < 0) {
                    throw new IllegalArgumentException("The price of \"" + v + "\" can't be negative");
                }
                priced |= value.price() != null;
                values.add(new ProductOption.OptionValue(v, value.price(), value.inStock()));
            }
            if (optionName == null && values.isEmpty()) {
                continue;
            }
            if (optionName == null) {
                throw new IllegalArgumentException("Every option needs a name");
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Option \"" + optionName + "\" has no values");
            }
            if (values.size() > MAX_VALUES) {
                throw new IllegalArgumentException("Option \"" + optionName + "\" has more than " + MAX_VALUES + " values");
            }
            if (!names.add(optionName.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Option \"" + optionName + "\" is repeated");
            }
            if (priced) {
                pricedOptions++;
            }
            cleaned.add(new ProductOption(optionName, values));
        }
        if (cleaned.size() > MAX_OPTIONS) {
            throw new IllegalArgumentException("A product can have up to " + MAX_OPTIONS + " options");
        }
        if (pricedOptions > 1) {
            throw new IllegalArgumentException("Only one option can set prices");
        }
        return cleaned;
    }

    static List<ProductOption.ProductSpec> cleanSpecs(List<ProductOption.ProductSpec> raw) {
        List<ProductOption.ProductSpec> cleaned = new ArrayList<>();
        for (ProductOption.ProductSpec spec : raw == null ? List.<ProductOption.ProductSpec>of() : raw) {
            if (spec == null) {
                continue;
            }
            String specName = text(spec.name(), "spec name");
            String value = spec.value() == null || spec.value().isBlank() ? null : spec.value().strip();
            if (specName == null && value == null) {
                continue;
            }
            if (specName == null || value == null) {
                throw new IllegalArgumentException("Every spec needs a name and a value");
            }
            if (value.length() > 300) {
                throw new IllegalArgumentException("The value of \"" + specName + "\" is too long");
            }
            cleaned.add(new ProductOption.ProductSpec(specName, value));
        }
        if (cleaned.size() > MAX_SPECS) {
            throw new IllegalArgumentException("A product can have up to " + MAX_SPECS + " specs");
        }
        return cleaned;
    }

    /** Collapses spaces; null for blank; rejects over-long text. */
    private static String text(String value, String what) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip().replaceAll("\\s+", " ");
        if (stripped.length() > MAX_TEXT) {
            throw new IllegalArgumentException("The " + what + " \"" + stripped.substring(0, 20) + "…\" is too long");
        }
        return stripped;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    // ---- photos -------------------------------------------------------------------------------

    public ProductImage addImage(ProductImage.Content content) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalArgumentException("A product can have up to " + MAX_IMAGES + " photos");
        }
        ProductImage image = new ProductImage(this, images.size(), content);
        images.add(image);
        return image;
    }

    /** @return false when the photo isn't this product's */
    public boolean removeImage(UUID imageId) {
        boolean removed = images.removeIf(i -> imageId.equals(i.getId()));
        renumber();
        return removed;
    }

    /** The listed ids first, in that order; photos not listed keep their relative order after them. */
    public void reorderImages(List<UUID> order) {
        List<ProductImage> sorted = new ArrayList<>(images);
        sorted.sort(Comparator.comparingInt(i -> {
            int index = order.indexOf(i.getId());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        images.clear();
        images.addAll(sorted);
        renumber();
    }

    private void renumber() {
        for (int i = 0; i < images.size(); i++) {
            images.get(i).moveTo(i);
        }
    }

    // ---- getters ------------------------------------------------------------------------------

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

    public boolean isInStock() {
        return inStock;
    }

    public List<ProductOption> getOptions() {
        return List.copyOf(options);
    }

    public List<ProductOption.ProductSpec> getSpecs() {
        return List.copyOf(specs);
    }

    public List<ProductImage> getImages() {
        return List.copyOf(images);
    }

    /** The editable fields, validated by the caller (plus the option rules in {@link #apply}). */
    public record Details(String name, String category, String description, BigDecimal price, String currency,
                          List<ProductOption> options, List<ProductOption.ProductSpec> specs, boolean inStock) {
    }
}
