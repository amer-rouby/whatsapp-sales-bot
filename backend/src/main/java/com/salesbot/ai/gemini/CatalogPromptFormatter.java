package com.salesbot.ai.gemini;

import com.salesbot.ai.ReplyRequest.Option;
import com.salesbot.ai.ReplyRequest.OptionValue;
import com.salesbot.ai.ReplyRequest.ProductFact;
import com.salesbot.ai.ReplyRequest.Spec;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** Renders the store's products as the "only truth" section of the system instruction. */
final class CatalogPromptFormatter {

    static final String HEADER = "منتجات المحل (دي الحاجات الوحيدة الموجودة، وأي حاجة مش هنا مش موجودة):";
    static final String EMPTY = "منتجات المحل: مفيش أي منتجات مسجلة حالياً. "
            + "لو العميل سأل عن أي منتج أو سعر، قوله بلطف إنك هتتأكد من الفريق وترد عليه، ومتأكدش وجود أي حاجة.";

    private CatalogPromptFormatter() {
    }

    static String format(List<ProductFact> products) {
        if (products.isEmpty()) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder(HEADER);
        for (ProductFact p : products) {
            // the name stands alone in «» so the model copies exactly it into orders and photos (not the category)
            sb.append("\n- «").append(p.name()).append('»');
            if (p.category() != null) {
                sb.append(" | القسم: ").append(p.category());
            }
            boolean optionPrices = p.options().stream().flatMap(o -> o.values().stream()).anyMatch(v -> v.price() != null);
            sb.append(" | السعر: ").append(plain(p.price())).append(' ').append(p.currency());
            if (optionPrices) {
                sb.append(" (إلا لو الاختيار ليه سعر مكتوب جنبه)");
            }
            for (Option option : p.options()) {
                sb.append(" | ").append(option.name()).append(": ").append(values(option.values(), p.currency()));
            }
            for (Spec spec : p.specs()) {
                sb.append(" | ").append(spec.name()).append(": ").append(spec.value());
            }
            sb.append(" | ").append(p.inStock() ? "متاح" : "خلص (مش متاح دلوقتي)");
            sb.append(" | ").append(p.hasPhoto() ? "ليه صورة" : "مالوش صورة");
            if (p.description() != null) {
                sb.append(" | ").append(p.description());
            }
        }
        return sb.toString();
    }

    /** "128GB (20000 EGP)، 256GB (23000 EGP، خلص)" */
    private static String values(List<OptionValue> values, String currency) {
        return values.stream().map(v -> {
            String note = v.price() != null ? plain(v.price()) + " " + currency : null;
            if (!v.inStock()) {
                note = note == null ? "خلص" : note + "، خلص";
            }
            return note == null ? v.value() : v.value() + " (" + note + ")";
        }).collect(Collectors.joining("، "));
    }

    /** 1250.00 → 1250, 99.50 → 99.5 */
    private static String plain(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }
}
