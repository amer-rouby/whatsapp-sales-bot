package com.salesbot.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.ReplyRequest;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.OutboundImage;
import com.salesbot.messaging.OutboundMessageSender;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** The bot sends a product's photo when asked, and prices an order by the chosen option. */
@IntegrationTest
@WithMockUser
@AutoConfigureMockMvc
class ProductPhotoReplyIntegrationTest {

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
                "wamid." + UUID.randomUUID(), "PN_PHOTO", customer, "Mona", text, Instant.now())));
    }

    @Test
    void sendsTheMainPhotoAndShowsItInTheChat() throws Exception {
        String productId = JsonPath.read(mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ساعة اختبار\",\"price\":900,\"currency\":\"EGP\",\"inStock\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        String imageId = JsonPath.read(mvc.perform(multipart("/api/products/" + productId + "/images")
                        .file(new MockMultipartFile("file", "w.png", "image/png", png())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        ArgumentCaptor<ReplyRequest> request = ArgumentCaptor.forClass(ReplyRequest.class);
        when(ai.reply(request.capture())).thenReturn(new AiReply("دي الساعة 👇",
                List.of("«ساعة اختبار»", "منتج مش موجود"), null));

        customerSays("201000000301", "ممكن أشوف الساعة؟");

        ArgumentCaptor<OutboundImage> photo = ArgumentCaptor.forClass(OutboundImage.class);
        verify(sender, timeout(5_000)).sendText("PN_PHOTO", "201000000301", "دي الساعة 👇");
        verify(sender, timeout(5_000)).sendImage(eq("PN_PHOTO"), eq("201000000301"), photo.capture());
        assertThat(photo.getValue().cacheKey()).isEqualTo("product-image:" + imageId);
        assertThat(photo.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(photo.getValue().caption()).isEqualTo("ساعة اختبار");
        assertThat(photo.getValue().data()).isNotEmpty();
        assertThat(request.getValue().products()).anyMatch(p -> p.name().equals("ساعة اختبار") && p.hasPhoto());

        // the inbox shows the photo, and the AI history says it was sent
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> JsonPath.<List<?>>read(
                mvc.perform(get("/api/conversations")).andReturn().getResponse().getContentAsString(),
                "$[?(@.customerWaId=='201000000301' && @.messageCount==3)]").size() == 1);
        String chats = mvc.perform(get("/api/conversations")).andReturn().getResponse().getContentAsString();
        String chatId = JsonPath.<List<String>>read(chats, "$[?(@.customerWaId=='201000000301')].id").get(0);
        String detail = mvc.perform(get("/api/conversations/" + chatId)).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(detail, "$.messages[2].imageId")).isEqualTo(imageId);
        assertThat(JsonPath.<String>read(detail, "$.messages[2].body")).isEqualTo("[صورة: «ساعة اختبار»]");
        assertThat(JsonPath.<Object>read(detail, "$.messages[1].imageId")).isNull();
    }

    @Test
    void ordersArePricedByTheChosenOption() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"موبايل الأوردر","price":20000,"currency":"EGP","inStock":true,"options":[
                          {"name":"السعة","values":[{"value":"128GB"},{"value":"256GB","price":23000}]},
                          {"name":"اللون","values":[{"value":"أسود"},{"value":"أزرق","inStock":false}]}]}"""))
                .andExpect(status().isCreated());

        when(ai.reply(any())).thenReturn(new AiReply("تم", List.of(), new AiReply.OrderDraft("منى", "القاهرة", List.of(
                new AiReply.OrderDraft.Item("موبايل الأوردر", List.of(
                        new AiReply.OrderDraft.Choice("السعة", "256GB"),
                        new AiReply.OrderDraft.Choice("اللون", "أسود")), 2)))));
        customerSays("201000000302", "أكد");

        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> JsonPath.<List<?>>read(
                mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString(),
                "$[?(@.customerPhone=='201000000302')]").size() == 1);
        String orders = mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Double>>read(orders, "$[?(@.customerPhone=='201000000302')].total")).containsExactly(46000.0);
        assertThat(JsonPath.<List<Boolean>>read(orders, "$[?(@.customerPhone=='201000000302')].needsReview")).containsExactly(false);
        assertThat(JsonPath.<List<String>>read(orders, "$[?(@.customerPhone=='201000000302')].items[0].options[0].value"))
                .containsExactly("256GB");

        // a sold-out colour → recorded, but flagged for a person to check
        when(ai.reply(any())).thenReturn(new AiReply("تم", List.of(), new AiReply.OrderDraft("منى", "القاهرة", List.of(
                new AiReply.OrderDraft.Item("موبايل الأوردر", List.of(
                        new AiReply.OrderDraft.Choice("السعة", "128GB"),
                        new AiReply.OrderDraft.Choice("اللون", "أزرق")), 1)))));
        customerSays("201000000303", "أكد");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> JsonPath.<List<?>>read(
                mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString(),
                "$[?(@.customerPhone=='201000000303')]").size() == 1);
        String flagged = mvc.perform(get("/api/orders")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Boolean>>read(flagged, "$[?(@.customerPhone=='201000000303')].needsReview")).containsExactly(true);
        assertThat(JsonPath.<List<Double>>read(flagged, "$[?(@.customerPhone=='201000000303')].total")).containsExactly(20000.0);
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
