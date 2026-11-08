package com.salesbot.messaging.infrastructure;

import com.salesbot.messaging.OutboundImage;
import com.salesbot.messaging.OutboundMessageSender;
import com.salesbot.messaging.simulator.SimulatorOutbox;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Sends messages through the WhatsApp Cloud API: {@code POST {base}/{version}/{phone-number-id}/messages}.
 * Photos are uploaded once ({@code /{phone-number-id}/media}) and sent by media id, reused while Meta keeps them.
 */
@Component
@ConditionalOnProperty(name = "salesbot.whatsapp.sender", havingValue = "cloud")
class CloudApiMessageSender implements OutboundMessageSender {

    private static final Logger log = LoggerFactory.getLogger(CloudApiMessageSender.class);
    /** WhatsApp's limit for an image caption. */
    private static final int MAX_CAPTION = 1024;

    private final RestClient restClient;
    private final SimulatorOutbox simulatorOutbox;
    private final MediaCache mediaCache;

    CloudApiMessageSender(RestClient.Builder builder, WhatsAppProperties properties, SimulatorOutbox simulatorOutbox,
                          MediaCache mediaCache) {
        this.simulatorOutbox = simulatorOutbox;
        this.mediaCache = mediaCache;
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
        send(phoneNumberId, to, "text", Map.of("preview_url", false, "body", text));
    }

    @Override
    public void sendImage(String phoneNumberId, String to, OutboundImage image) {
        if (simulatorOutbox.accepts(phoneNumberId)) {
            simulatorOutbox.recordImage(to, image);
            return;
        }
        var cached = mediaCache.find(image.cacheKey(), phoneNumberId);
        String mediaId = cached.orElseGet(() -> upload(phoneNumberId, image));
        try {
            send(phoneNumberId, to, "image", imageBody(mediaId, image.caption()));
        } catch (RestClientResponseException e) {
            if (cached.isEmpty() || !e.getStatusCode().is4xxClientError()) {
                throw e;
            }
            // Meta may drop a file before its 30 days are up: upload again once and retry
            log.warn("[WHATSAPP-OUT] cached photo {} was refused, uploading it again", image.cacheKey());
            mediaCache.forget(image.cacheKey(), phoneNumberId);
            send(phoneNumberId, to, "image", imageBody(upload(phoneNumberId, image), image.caption()));
        }
    }

    private static Map<String, Object> imageBody(String mediaId, String caption) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", mediaId);
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption.length() > MAX_CAPTION ? caption.substring(0, MAX_CAPTION) : caption);
        }
        return body;
    }

    private String upload(String phoneNumberId, OutboundImage image) {
        String filename = image.contentType().endsWith("png") ? "photo.png" : "photo.jpg";
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(image.contentType()));
        fileHeaders.setContentDisposition(ContentDisposition.formData().name("file").filename(filename).build());
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("messaging_product", "whatsapp");
        parts.add("type", image.contentType());
        parts.add("file", new HttpEntity<>(new ByteArrayResource(image.data()), fileHeaders));
        try {
            MediaUpload uploaded = restClient.post()
                    .uri("/{phoneNumberId}/media", phoneNumberId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(MediaUpload.class);
            if (uploaded == null || uploaded.id() == null) {
                throw new IllegalStateException("WhatsApp returned no media id for " + image.cacheKey());
            }
            mediaCache.remember(image.cacheKey(), phoneNumberId, uploaded.id());
            log.info("[WHATSAPP-OUT] uploaded photo {} ({} bytes)", image.cacheKey(), image.data().length);
            return uploaded.id();
        } catch (RestClientResponseException e) {
            log.error("[WHATSAPP-OUT] WhatsApp refused the photo upload {}: {} {}",
                    image.cacheKey(), e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private void send(String phoneNumberId, String to, String type, Map<String, Object> content) {
        try {
            restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", to,
                            "type", type,
                            type, content))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WHATSAPP-OUT] sent {} to={}", type, to);
        } catch (RestClientResponseException e) {
            // Meta explains the problem in the body (expired token, number not in the test list, ...)
            log.error("[WHATSAPP-OUT] WhatsApp refused the {} message to {}: {} {}",
                    type, to, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    record MediaUpload(String id) {
    }
}
