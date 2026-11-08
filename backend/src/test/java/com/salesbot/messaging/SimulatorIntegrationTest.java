package com.salesbot.messaging;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.salesbot.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** The simulator runs a message through the real pipeline and returns the bot's reply. */
@IntegrationTest
@WithMockUser // these tests are about the feature, not about login (see SecurityIntegrationTest)
@TestPropertySource(properties = "salesbot.simulator.enabled=true")
@AutoConfigureMockMvc
class SimulatorIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsTheBotReply() throws Exception {
        mvc.perform(post("/dev/simulator/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"sim-test-1\",\"customerName\":\"Mona\",\"text\":\"السلام عليكم\"}"))
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
}
