package com.salesbot.conversation;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import com.salesbot.messaging.InboundMessageReceived;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** The shop answers a customer from the dashboard: the reply is sent, and the bot stays quiet until handed back. */
@IntegrationTest
@WithMockUser
@AutoConfigureMockMvc
class AgentReplyIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    private void customerSays(String customer, String text) {
        tx.executeWithoutResult(s -> events.publishEvent(new InboundMessageReceived(
                "wamid." + UUID.randomUUID(), "PN_AGENT", customer, "Hoda", text, Instant.now())));
    }

    private String idOf(String customer) throws Exception {
        String list = mvc.perform(get("/api/conversations")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(list, "$[?(@.customerWaId=='" + customer + "')].id");
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int messageCount(String id) throws Exception {
        String detail = mvc.perform(get("/api/conversations/" + id)).andReturn().getResponse().getContentAsString();
        return JsonPath.<Integer>read(detail, "$.messages.length()");
    }

    /** A chat where the customer wrote once and the bot answered. */
    private String startChat(String customer) throws Exception {
        customerSays(customer, "عايز أسأل");
        await().pollInSameThread().atMost(Duration.ofSeconds(5))
                .until(() -> idOf(customer) != null && messageCount(idOf(customer)) == 2);
        return idOf(customer);
    }

    private void reply(String id, String json, int expectedStatus) throws Exception {
        mvc.perform(post("/api/conversations/" + id + "/messages").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    private void setBot(String id, String json, int expectedStatus) throws Exception {
        mvc.perform(put("/api/conversations/" + id + "/bot").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void replyIsStoredAsTheShopsAndPausesTheBotUntilHandedBack() throws Exception {
        String id = startChat("201000000201");

        mvc.perform(post("/api/conversations/" + id + "/messages")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  أهلاً، معاكي صاحب المحل  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("AGENT"))
                .andExpect(jsonPath("$.direction").value("OUTBOUND"))
                .andExpect(jsonPath("$.body").value("أهلاً، معاكي صاحب المحل"));
        mvc.perform(get("/api/conversations/" + id))
                .andExpect(jsonPath("$.botPaused").value(true))
                .andExpect(jsonPath("$.canReply").value(true))
                .andExpect(jsonPath("$.messages[0].author").value("CUSTOMER"))
                .andExpect(jsonPath("$.messages[1].author").value("BOT"))
                .andExpect(jsonPath("$.messages[2].author").value("AGENT"));
        mvc.perform(get("/api/conversations"))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].botPaused").value(true));

        // bot paused: the customer's message is stored, nobody auto-answers it
        customerSays("201000000201", "تمام");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> messageCount(id) == 4);
        await().pollInSameThread().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .until(() -> messageCount(id) == 4);

        // handed back: the bot answers again
        setBot(id, "{\"paused\":false}", 204);
        customerSays("201000000201", "بكام؟");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> messageCount(id) == 6);
        mvc.perform(get("/api/conversations/" + id))
                .andExpect(jsonPath("$.botPaused").value(false))
                .andExpect(jsonPath("$.messages[5].author").value("BOT"));
    }

    @Test
    void pausingByHandStopsTheBotToo() throws Exception {
        String id = startChat("201000000204");
        setBot(id, "{\"paused\":true}", 204);
        customerSays("201000000204", "فيه خصم؟");
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> messageCount(id) == 3);
        await().pollInSameThread().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .until(() -> messageCount(id) == 3);
    }

    @Test
    void refusesAReplyAfterWhatsApps24HourWindow() throws Exception {
        String id = startChat("201000000202");
        jdbc.update("UPDATE messages SET created_at = now() - interval '25 hours' WHERE conversation_id = ?::uuid", id);

        mvc.perform(get("/api/conversations/" + id)).andExpect(jsonPath("$.canReply").value(false));
        reply(id, "{\"text\":\"لسه مهتم؟\"}", 409);
        mvc.perform(get("/api/conversations/" + id))
                .andExpect(jsonPath("$.botPaused").value(false))
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    @Test
    void rejectsEmptyOrTooLongRepliesAndUnknownChats() throws Exception {
        String id = startChat("201000000203");
        reply(id, "{\"text\":\"   \"}", 400);
        reply(id, "{\"text\":\"" + "a".repeat(4097) + "\"}", 400);
        reply(UUID.randomUUID().toString(), "{\"text\":\"hi\"}", 404);
        setBot(UUID.randomUUID().toString(), "{\"paused\":true}", 404);
        setBot(id, "{}", 400);
    }
}
