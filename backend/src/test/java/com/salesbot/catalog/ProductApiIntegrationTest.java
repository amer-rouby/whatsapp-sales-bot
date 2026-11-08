package com.salesbot.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@WithMockUser // these tests are about the feature, not about login (see SecurityIntegrationTest)
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

    private static final String SHOE = """
            {"name":" كوتشي أبيض ","category":"أحذية","price":1250,"currency":"egp",
             "sizes":"40, 41, 42","colors":"أبيض","inStock":true,"description":" "}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogQuery catalog;

    @Test
    void fullLifecycle() throws Exception {
        String body = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(SHOE))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("كوتشي أبيض"))   // trimmed
                .andExpect(jsonPath("$.currency").value("EGP"))      // upper-cased
                .andExpect(jsonPath("$.description").doesNotExist()) // blank → null
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // the bot sees it right away
        assertThat(catalog.products()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("كوتشي أبيض");
            assertThat(p.price()).isEqualByComparingTo(new BigDecimal("1250"));
            assertThat(p.inStock()).isTrue();
        });

        mvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(SHOE.replace("\"inStock\":true", "\"inStock\":false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(false));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].inStock").value(false));

        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsInvalidProduct() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-5,\"currency\":\"EG\",\"inStock\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void dashboardOriginMayCallTheApi() throws Exception {
        mvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mvc.perform(options("/api/products")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
