package com.salesbot.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.salesbot.IntegrationTest;
import com.salesbot.conversation.infrastructure.MessageRepository;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@TestPropertySource(properties = {
        "salesbot.whatsapp.verify-token=test-verify",
        "salesbot.whatsapp.app-secret=test-secret",
        "salesbot.whatsapp.verify-signature=true"
})
@AutoConfigureMockMvc
class WhatsAppWebhookIntegrationTest {

    private static final String PAYLOAD = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "WABA_1",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "metadata": {"display_phone_number": "201000000000", "phone_number_id": "PN_1"},
                    "contacts": [{"profile": {"name": "Ahmed"}, "wa_id": "201111111111"}],
                    "messages": [{
                      "from": "201111111111",
                      "id": "wamid.TEST1",
                      "timestamp": "1762500000",
                      "type": "text",
                      "text": {"body": "بكام الجزمة مقاس 42؟"}
                    }]
                  }
                }]
              }]
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    MessageRepository messageRepository;

    @MockitoBean
    OutboundMessageSender sender;

    @Test
    void verificationHandshakeEchoesChallenge() throws Exception {
        mvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("12345"));

        mvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnsignedOrForgedCalls() throws Exception {
        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD)
                        .header("X-Hub-Signature-256", "sha256=" + "00".repeat(32)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repliesOnceEvenWhenMetaRetries() throws Exception {
        byte[] body = PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + HexFormat.of().formatHex(hmac("test-secret", body));

        for (int i = 0; i < 2; i++) { // second call simulates a webhook retry
            mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body)
                            .header("X-Hub-Signature-256", signature))
                    .andExpect(status().isOk());
        }

        verify(sender, timeout(5_000).times(1))
                .sendText("PN_1", "201111111111", "وصلتني رسالتك: بكام الجزمة مقاس 42؟");
        verify(sender, times(1)).sendText(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertThat(messageRepository.existsByWaMessageId("wamid.TEST1")).isTrue();
    }

    private static byte[] hmac(String secret, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }
}
