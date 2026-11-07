package com.salesbot.messaging.application;

import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.api.WebhookPayload;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a webhook body into {@link InboundMessageReceived} events.
 * The webhook must answer Meta fast, so all real work happens in async listeners
 * after this transaction commits.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final ApplicationEventPublisher events;

    WebhookService(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Transactional
    public void handle(WebhookPayload payload) {
        if (payload.entry() == null) {
            return;
        }
        payload.entry().stream()
                .filter(e -> e.changes() != null)
                .flatMap(e -> e.changes().stream())
                .filter(c -> "messages".equals(c.field()) && c.value() != null)
                .forEach(c -> publishMessages(c.value()));
    }

    private void publishMessages(WebhookPayload.Value value) {
        if (value.messages() == null || value.metadata() == null) {
            return; // status updates (sent/delivered/read) — ignored for now
        }
        String customerName = firstContactName(value.contacts());
        for (WebhookPayload.Message message : value.messages()) {
            if (!"text".equals(message.type()) || message.text() == null) {
                log.info("Skipping unsupported message type {}", message.type());
                continue;
            }
            events.publishEvent(new InboundMessageReceived(
                    message.id(),
                    value.metadata().phoneNumberId(),
                    message.from(),
                    customerName,
                    message.text().body(),
                    parseEpochSeconds(message.timestamp())));
        }
    }

    private static String firstContactName(List<WebhookPayload.Contact> contacts) {
        if (contacts == null) {
            return null;
        }
        return contacts.stream()
                .map(WebhookPayload.Contact::profile)
                .filter(Objects::nonNull)
                .map(WebhookPayload.Profile::name)
                .findFirst()
                .orElse(null);
    }

    private static Instant parseEpochSeconds(String timestamp) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException | NullPointerException e) {
            return Instant.now();
        }
    }
}
