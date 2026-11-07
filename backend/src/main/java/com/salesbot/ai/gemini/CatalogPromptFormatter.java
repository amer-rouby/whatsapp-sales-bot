package com.salesbot.ai.gemini;

import com.salesbot.ai.ReplyRequest.ProductFact;
import java.math.BigDecimal;
import java.util.List;

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
            // the name stands alone in «» so the model copies exactly it into orders (not the category)
            sb.append("\n- «").append(p.name()).append('»');
            if (p.category() != null) {
                sb.append(" | القسم: ").append(p.category());
            }
            sb.append(" | السعر: ").append(plain(p.price())).append(' ').append(p.currency());
            if (p.sizes() != null) {
                sb.append(" | المقاسات: ").append(p.sizes());
            }
            if (p.colors() != null) {
                sb.append(" | الألوان: ").append(p.colors());
            }
            sb.append(" | ").append(p.inStock() ? "متاح" : "خلص (مش متاح دلوقتي)");
            if (p.description() != null) {
                sb.append(" | ").append(p.description());
            }
        }
        return sb.toString();
    }

    /** 1250.00 → 1250, 99.50 → 99.5 */
    private static String plain(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }
}
