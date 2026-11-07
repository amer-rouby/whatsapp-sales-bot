package com.salesbot.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The AI names products in orders; these are the variations seen in real runs. */
class ProductNameMatchingTest {

    @Test
    void cleansQuotesAndSpaces() {
        assertThat(ProductService.cleanName("  «كوتشي   رياضي أبيض» ")).isEqualTo("كوتشي رياضي أبيض");
        assertThat(ProductService.cleanName(null)).isEmpty();
    }

    @Test
    void dropsATrailingNoteInBrackets() {
        assertThat(ProductService.withoutTrailingNote("كوتشي رياضي أبيض (أحذية)")).isEqualTo("كوتشي رياضي أبيض");
        assertThat(ProductService.withoutTrailingNote("كوتشي [مقاس 42]")).isEqualTo("كوتشي");
        assertThat(ProductService.withoutTrailingNote("تيشيرت (قطن) أبيض")).isEqualTo("تيشيرت (قطن) أبيض");
    }
}
