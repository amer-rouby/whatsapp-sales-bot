package com.salesbot.messaging.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code salesbot.whatsapp.*}. */
@ConfigurationProperties("salesbot.whatsapp")
public record WhatsAppProperties(
        String verifyToken,
        String appSecret,
        boolean verifySignature,
        String accessToken,
        String apiBaseUrl,
        String apiVersion,
        String sender) {
}
