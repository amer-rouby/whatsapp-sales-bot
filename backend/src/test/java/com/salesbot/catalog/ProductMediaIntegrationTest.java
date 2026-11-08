package com.salesbot.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Products of any kind: options with their own prices and stock, specs, and photos. */
@IntegrationTest
@WithMockUser
@AutoConfigureMockMvc
class ProductMediaIntegrationTest {

    private static final String PHONE = """
            {"name":"موبايل اختبار %s","category":"موبايلات","price":20000,"currency":"egp","inStock":true,
             "options":[
               {"name":" السعة ","values":[{"value":"128GB"},{"value":"256GB","price":23000},{"value":"512GB","price":27000,"inStock":false}]},
               {"name":"اللون","values":[{"value":"أسود"},{"value":" "},{"value":"أزرق"}]},
               {"name":"","values":[]}
             ],
             "specs":[{"name":"الضمان","value":"سنة"},{"name":"","value":""}]}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogQuery catalog;

    private String createPhone(String suffix) throws Exception {
        String body = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void storesCleanOptionsAndSpecs() throws Exception {
        String id = createPhone("1");
        mvc.perform(get("/api/products/" + id))
                .andExpect(jsonPath("$.currency").value("EGP"))
                .andExpect(jsonPath("$.options.length()").value(2))          // the empty row is dropped
                .andExpect(jsonPath("$.options[0].name").value("السعة"))     // trimmed
                .andExpect(jsonPath("$.options[0].values[1].price").value(23000))
                .andExpect(jsonPath("$.options[0].values[2].inStock").value(false))
                .andExpect(jsonPath("$.options[0].values[0].inStock").value(true)) // default
                .andExpect(jsonPath("$.options[1].values.length()").value(2))  // blank value dropped
                .andExpect(jsonPath("$.specs.length()").value(1))
                .andExpect(jsonPath("$.specs[0].value").value("سنة"))
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    void refusesOptionsTheBotCouldNotPriceClearly() throws Exception {
        String twoPricedOptions = """
                {"name":"x","price":1,"currency":"EGP","inStock":true,"options":[
                  {"name":"السعة","values":[{"value":"128","price":5}]},
                  {"name":"اللون","values":[{"value":"أسود","price":6}]}]}""";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(twoPricedOptions))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only one option can set prices"));

        String repeated = """
                {"name":"x","price":1,"currency":"EGP","inStock":true,"options":[
                  {"name":"اللون","values":[{"value":"أسود"}]},{"name":"اللون ","values":[{"value":"أبيض"}]}]}""";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(repeated))
                .andExpect(status().isBadRequest());

        String noValues = """
                {"name":"x","price":1,"currency":"EGP","inStock":true,"options":[{"name":"اللون","values":[]}]}""";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(noValues))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pricesFollowTheChosenOption() throws Exception {
        createPhone("2");
        String name = "موبايل اختبار 2";

        CatalogQuery.Quote base = catalog.quote(name, List.of(choice("السعة", "128GB"), choice("اللون", "أسود"))).orElseThrow();
        assertThat(base.unitPrice()).isEqualByComparingTo("20000");
        assertThat(base.valid()).isTrue();

        CatalogQuery.Quote bigger = catalog.quote(name, List.of(choice(" السعة", "256gb"))).orElseThrow();
        assertThat(bigger.unitPrice()).isEqualByComparingTo(new BigDecimal("23000"));
        assertThat(bigger.valid()).isTrue();

        assertThat(catalog.quote(name, List.of(choice("السعة", "512GB"))).orElseThrow().valid()).isFalse(); // sold out
        assertThat(catalog.quote(name, List.of(choice("السعة", "1TB"))).orElseThrow().valid()).isFalse();   // unknown
        assertThat(catalog.quote(name, List.of(choice("اللون", "أسود"))).orElseThrow().valid()).isFalse();  // price not chosen
        assertThat(catalog.quote(name, List.of(choice("السعة", "128GB"), choice("الخامة", "جلد")))
                .orElseThrow().valid()).isFalse(); // option the product doesn't have
        assertThat(catalog.quote("مش موجود", List.of())).isEmpty();
    }

    @Test
    void photosAreProcessedServedOrderedAndRemoved() throws Exception {
        String id = createPhone("3");

        // a big transparent PNG becomes a 1600px JPEG on white, plus a 400px thumbnail
        String first = upload(id, png(2400, 1200, true)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.width").value(1600))
                .andExpect(jsonPath("$.height").value(800))
                .andReturn().getResponse().getContentAsString();
        String firstId = JsonPath.read(first, "$.id");
        String secondId = JsonPath.read(upload(id, png(300, 300, false)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        byte[] full = mvc.perform(get("/api/products/images/" + firstId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(full));
        assertThat(decoded.getWidth()).isEqualTo(1600);
        assertThat(new Color(decoded.getRGB(5, 5))).isEqualTo(Color.WHITE); // transparency flattened onto white

        byte[] thumb = mvc.perform(get("/api/products/images/" + firstId).param("size", "thumb"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(thumb)).getWidth()).isEqualTo(400);

        // the main photo is the first one, until the owner reorders
        assertThat(catalog.mainPhoto("موبايل اختبار 3").orElseThrow().imageId().toString()).isEqualTo(firstId);
        mvc.perform(put("/api/products/" + id + "/images/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageIds\":[\"" + secondId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].id").value(secondId))
                .andExpect(jsonPath("$.images[1].id").value(firstId));
        assertThat(catalog.mainPhoto("موبايل اختبار 3").orElseThrow().imageId().toString()).isEqualTo(secondId);
        assertThat(catalog.findByName("موبايل اختبار 3").orElseThrow().imageCount()).isEqualTo(2);

        mvc.perform(delete("/api/products/" + id + "/images/" + secondId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/images/" + secondId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.images.length()").value(1));

        // deleting the product removes its photos too
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/images/" + firstId)).andExpect(status().isNotFound());
    }

    @Test
    void refusesFilesThatAreNotPhotosAndTooManyPhotos() throws Exception {
        String id = createPhone("4");
        upload(id, "not an image".getBytes()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only JPEG or PNG photos are accepted"));
        upload(id, png(4100, 10, false)).andExpect(status().isBadRequest());

        byte[] small = png(50, 50, false);
        for (int i = 0; i < 8; i++) {
            upload(id, small).andExpect(status().isCreated());
        }
        upload(id, small).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A product can have up to 8 photos"));
    }

    private org.springframework.test.web.servlet.ResultActions upload(String productId, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/products/" + productId + "/images")
                .file(new MockMultipartFile("file", "photo.png", "image/png", bytes)));
    }

    private static ProductOption.OptionChoice choice(String name, String value) {
        return new ProductOption.OptionChoice(name, value);
    }

    static byte[] png(int width, int height, boolean transparentCorner) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 90, 200));
        g.fillRect(0, 0, width, height);
        if (transparentCorner) {
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, width / 4, height / 4);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
