package com.salesbot.conversation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salesbot.IntegrationTest;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.AiUnavailableException;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.OutboundMessageSender;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/** When the AI is down the customer still gets an answer, never silence. */
@IntegrationTest
class AiOutageIntegrationTest {

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @MockitoBean
    AiReplyProvider aiReplyProvider;

    @MockitoBean
    OutboundMessageSender sender;

    @Test
    void sendsFallbackReplyWhenAiIsUnavailable() {
        when(aiReplyProvider.reply(any())).thenThrow(new AiUnavailableException("429 rate limited"));

        // published inside a transaction, exactly like the webhook does
        tx.executeWithoutResult(status -> events.publishEvent(new InboundMessageReceived(
                "wamid.OUTAGE1", "PN_2", "201222222222", "Sara", "بكام الشنطة؟", Instant.now())));

        verify(sender, timeout(5_000)).sendText("PN_2", "201222222222",
                "أهلاً بيك 🙏 ثواني وحد من فريقنا هيرد عليك.");
    }
}
