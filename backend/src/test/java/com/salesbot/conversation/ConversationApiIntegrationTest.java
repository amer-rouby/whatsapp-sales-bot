package com.salesbot.conversation;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.SimulatorChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** The dashboard inbox lists every chat (latest first) and shows a chat's full thread in order. */
@IntegrationTest
@WithMockUser
@AutoConfigureMockMvc
class ConversationApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    private void customerSays(String phoneNumberId, String customer, String name, String text) {
        tx.executeWithoutResult(s -> events.publishEvent(new InboundMessageReceived(
                "wamid." + UUID.randomUUID(), phoneNumberId, customer, name, text, Instant.now())));
    }

    private String list() throws Exception {
        return mvc.perform(get("/api/conversations")).andReturn().getResponse().getContentAsString();
    }

    private String idOf(String customer) throws Exception {
        List<String> ids = JsonPath.read(list(), "$[?(@.customerWaId=='" + customer + "')].id");
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Test
    void listsChatsWithTheirLatestMessageAndShowsTheThread() throws Exception {
        customerSays("PN_CONV", "201000000101", "Mona", "السلام عليكم");
        // echo provider answers each message; wait for message + reply
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> {
            String id = idOf("201000000101");
            return id != null && JsonPath.<Integer>read(
                    mvc.perform(get("/api/conversations/" + id)).andReturn().getResponse().getContentAsString(),
                    "$.messages.length()") == 2;
        });
        customerSays("PN_CONV", "201000000101", "Mona", "بكام الكوتشي؟");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> JsonPath.<Integer>read(
                mvc.perform(get("/api/conversations/" + idOf("201000000101"))).andReturn().getResponse()
                        .getContentAsString(), "$.messages.length()") == 4);

        String id = idOf("201000000101");
        mvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].customerName").value("Mona"))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].simulated").value(false))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].messageCount").value(4))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].lastDirection").value("OUTBOUND"))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].lastMessage").value("وصلتني رسالتك: بكام الكوتشي؟"));

        mvc.perform(get("/api/conversations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].direction").value("INBOUND"))
                .andExpect(jsonPath("$.messages[0].body").value("السلام عليكم"))
                .andExpect(jsonPath("$.messages[1].direction").value("OUTBOUND"))
                .andExpect(jsonPath("$.messages[3].body").value("وصلتني رسالتك: بكام الكوتشي؟"));
    }

    @Test
    void marksSimulatorChats() throws Exception {
        customerSays(SimulatorChannel.PHONE_NUMBER_ID, "sim-conv-1", "Test", "hi");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> idOf("sim-conv-1") != null);
        mvc.perform(get("/api/conversations"))
                .andExpect(jsonPath("$[?(@.customerWaId=='sim-conv-1')].simulated").value(true));
    }

    @Test
    void unknownChatIs404() throws Exception {
        mvc.perform(get("/api/conversations/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
