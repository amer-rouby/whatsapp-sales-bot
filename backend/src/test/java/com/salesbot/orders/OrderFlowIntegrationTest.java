package com.salesbot.orders;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.OutboundMessageSender;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** Customer confirms in chat → order appears on the dashboard with catalog prices. */
@IntegrationTest
@WithMockUser // these tests are about the feature, not about login (see SecurityIntegrationTest)
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @MockitoBean
    AiReplyProvider ai;

    @MockitoBean
    OutboundMessageSender sender;

    private void customerSays(String customer, String text) {
        tx.executeWithoutResult(s -> events.publishEvent(new InboundMessageReceived(
                "wamid." + UUID.randomUUID(), "PN_ORD", customer, "Amer", text, Instant.now())));
    }

    private int ordersFor(String phone) throws Exception {
        String body = mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString();
        List<?> matches = JsonPath.read(body, "$[?(@.customerPhone=='" + phone + "')]");
        return matches.size();
    }

    @Test
    void confirmedOrderIsRecordedOnceWithCatalogPrices() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"كوتشي اختبار","price":1250,"currency":"EGP","inStock":true}"""))
                .andExpect(status().isCreated());

        // the AI "hallucinates" nothing about price: it only names products
        when(ai.reply(any())).thenReturn(new AiReply("تم تأكيد الأوردر", new AiReply.OrderDraft("عامر", "المنيا",
                List.of(new AiReply.OrderDraft.Item("كوتشي اختبار", "42", "أبيض", 2),
                        new AiReply.OrderDraft.Item("منتج مش موجود", null, null, 1)))));

        customerSays("201000000001", "أيوه أكد");
        verify(sender, timeout(5_000)).sendText("PN_ORD", "201000000001", "تم تأكيد الأوردر");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> ordersFor("201000000001") == 1);

        mvc.perform(get("/api/orders"))
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].customerName").value("عامر"))
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].shippingAddress").value("المنيا"))
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].status").value("NEW"))
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].total").value(2500.0))  // 2 × 1250
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].currency").value("EGP"))
                .andExpect(jsonPath("$[?(@.customerPhone=='201000000001')].needsReview").value(true));

        // customer confirms again a minute later → same order, not a second one
        customerSays("201000000001", "أكد تاني");
        verify(sender, timeout(5_000).times(2)).sendText("PN_ORD", "201000000001", "تم تأكيد الأوردر");
        Thread.sleep(300); // let the async listener finish
        org.assertj.core.api.Assertions.assertThat(ordersFor("201000000001")).isEqualTo(1);

        // owner moves it along
        String body = mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.<List<String>>read(body, "$[?(@.customerPhone=='201000000001')].id").get(0);
        mvc.perform(patch("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(patch("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replyWithoutOrderRecordsNothing() throws Exception {
        when(ai.reply(any())).thenReturn(AiReply.text("أهلاً بيك"));

        customerSays("201000000002", "السلام عليكم");
        verify(sender, timeout(5_000)).sendText("PN_ORD", "201000000002", "أهلاً بيك");
        verify(sender, times(1)).sendText(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(ordersFor("201000000002")).isZero();
    }
}
