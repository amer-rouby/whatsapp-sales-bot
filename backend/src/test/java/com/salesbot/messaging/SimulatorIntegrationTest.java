package com.salesbot.messaging;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.salesbot.IntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The simulator runs a message through the real pipeline and returns the bot's reply. */
@IntegrationTest
@WithMockUser // these tests are about the feature, not about login (see SecurityIntegrationTest)
@TestPropertySource(properties = "salesbot.simulator.enabled=true")
@AutoConfigureMockMvc
class SimulatorIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void returnsTheBotReply() throws Exception {
        send("sim-test-1", "السلام عليكم")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("وصلتني رسالتك: السلام عليكم"));
    }

    @Test
    void rejectsEmptyText() throws Exception {
        mvc.perform(post("/dev/simulator/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"sim-test-2\",\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatTakenOverByTheShopGetsNoBotReplyAndShowsTheShopsReply() throws Exception {
        send("sim-test-3", "عايز كوتشي").andExpect(jsonPath("$.reply").exists());
        UUID chat = jdbc.queryForObject(
                "SELECT id FROM conversations WHERE phone_number_id = ? AND customer_wa_id = ?",
                UUID.class, SimulatorChannel.PHONE_NUMBER_ID, "sim-test-3");

        // the shop answers from the dashboard: that pauses the bot, and the reply waits for the simulator
        mvc.perform(post("/api/conversations/" + chat + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"أهلاً، معاك المحل\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/dev/simulator/messages/sim-test-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies.length()").value(1))
                .andExpect(jsonPath("$.replies[0]").value("أهلاً، معاك المحل"));
        mvc.perform(get("/dev/simulator/messages/sim-test-3"))
                .andExpect(jsonPath("$.replies.length()").value(0));

        // the bot is paused: the customer's next message returns at once, without a reply
        send("sim-test-3", "بكام؟")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").doesNotExist());
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> customerMessages(chat) == 2);

        // handed back: the bot answers again
        mvc.perform(put("/api/conversations/" + chat + "/bot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paused\":false}"))
                .andExpect(status().isNoContent());
        send("sim-test-3", "تمام").andExpect(jsonPath("$.reply").value("وصلتني رسالتك: تمام"));
    }

    @Test
    void rejectsAnInvalidChatId() throws Exception {
        mvc.perform(get("/dev/simulator/messages/" + "x".repeat(40)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions send(String customerId, String text) throws Exception {
        return mvc.perform(post("/dev/simulator/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerId + "\",\"customerName\":\"Mona\",\"text\":\"" + text + "\"}"));
    }

    private int customerMessages(UUID chat) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM messages WHERE conversation_id = ? AND author = 'CUSTOMER'", Integer.class, chat);
    }
}
