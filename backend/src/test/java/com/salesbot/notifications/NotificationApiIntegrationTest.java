package com.salesbot.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.salesbot.IntegrationTest;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.orders.OrderPlacement;
import com.salesbot.orders.PlaceOrder;
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

/** The dashboard bell: new customer messages and new orders, newest first, with the server's clock. */
@IntegrationTest
@WithMockUser
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    private static final String MESSAGE_CUSTOMER = "201000000301";
    private static final String ORDER_CUSTOMER = "201000000302";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    OrderPlacement orders;

    private String feed() throws Exception {
        return mvc.perform(get("/api/notifications")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static <T> List<T> field(String body, String customer, String name) {
        return JsonPath.read(body, "$.items[?(@.customerContact=='" + customer + "')]." + name);
    }

    @Test
    void feedHasCustomerMessagesAndOrdersButNotTheBotsReplies() throws Exception {
        tx.executeWithoutResult(s -> events.publishEvent(new InboundMessageReceived(
                "wamid." + UUID.randomUUID(), "PN_NOTIFY", MESSAGE_CUSTOMER, "Salma", "عندكم مقاس 40؟", Instant.now())));
        // wait for the bot's reply too, to prove it does not show up in the bell
        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> JsonPath.<List<?>>read(
                mvc.perform(get("/api/conversations")).andReturn().getResponse().getContentAsString(),
                "$[?(@.customerWaId=='" + MESSAGE_CUSTOMER + "' && @.messageCount==2)]").size() == 1);

        orders.place(new PlaceOrder(null, ORDER_CUSTOMER, "Nour", "Cairo",
                List.of(new PlaceOrder.Item("منتج مش في الكتالوج", List.of(new PlaceOrder.OrderLineChoice("المقاس", "40")), 1))));

        String body = feed();
        assertThat(JsonPath.<String>read(body, "$.serverTime")).isNotBlank();
        assertThat(field(body, MESSAGE_CUSTOMER, "type")).containsExactly("CUSTOMER_MESSAGE");
        assertThat(field(body, MESSAGE_CUSTOMER, "preview")).containsExactly("عندكم مقاس 40؟");
        assertThat(field(body, MESSAGE_CUSTOMER, "customerName")).containsExactly("Salma");
        assertThat(field(body, ORDER_CUSTOMER, "type")).containsExactly("NEW_ORDER");
        // an item that is not in the catalog needs a person to review the order
        assertThat(field(body, ORDER_CUSTOMER, "needsAttention")).containsExactly(true);

        // newest first: the order came after the message
        List<String> customers = JsonPath.read(body, "$.items[*].customerContact");
        assertThat(customers.indexOf(ORDER_CUSTOMER)).isLessThan(customers.indexOf(MESSAGE_CUSTOMER));
    }

    @Test
    void pollingAfterTheLatestServerTimeSkipsOldItems() throws Exception {
        String future = Instant.now().plus(Duration.ofMinutes(5)).toString();
        mvc.perform(get("/api/notifications").param("after", future))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void invalidAfterIsABadRequest() throws Exception {
        mvc.perform(get("/api/notifications").param("after", "not-a-date")).andExpect(status().isBadRequest());
    }
}
