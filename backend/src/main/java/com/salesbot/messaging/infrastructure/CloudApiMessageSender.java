package com.salesbot.messaging.infrastructure;

import com.salesbot.messaging.OutboundMessageSender;
import com.salesbot.messaging.simulator.SimulatorOutbox;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Sends text messages through the WhatsApp Cloud API:
 * {@code POST {base}/{version}/{phone-number-id}/messages}.
 */
@Component
@ConditionalOnProperty(name = "salesbot.whatsapp.sender", havingValue = "cloud")
class CloudApiMessageSender implements OutboundMessageSender {

    private static final Logger log = LoggerFactory.getLogger(CloudApiMessageSender.class);

    private final RestClient restClient;
    private final SimulatorOutbox simulatorOutbox;

    CloudApiMessageSender(RestClient.Builder builder, WhatsAppProperties properties, SimulatorOutbox simulatorOutbox) {
        this.simulatorOutbox = simulatorOutbox;
        if (properties.accessToken() == null || properties.accessToken().isBlank()) {
            throw new IllegalStateException("salesbot.whatsapp.access-token is required when sender=cloud");
        }
        this.restClient = builder
                .baseUrl(properties.apiBaseUrl() + "/" + properties.apiVersion())
                .defaultHeader("Authorization", "Bearer " + properties.accessToken())
                .build();
    }

    @Override
    public void sendText(String phoneNumberId, String to, String text) {
        if (simulatorOutbox.accepts(phoneNumberId)) {
            simulatorOutbox.record(to, text); // dashboard chat: never goes to WhatsApp
            return;
        }
        try {
            restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", to,
                            "type", "text",
                            "text", Map.of("preview_url", false, "body", text)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WHATSAPP-OUT] sent to={}", to);
        } catch (RestClientResponseException e) {
            // Meta explains the problem in the body (expired token, number not in the test list, ...)
            log.error("[WHATSAPP-OUT] WhatsApp refused the message to {}: {} {}",
                    to, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
