package com.salesbot.messaging.api;

import com.salesbot.messaging.application.WebhookService;
import com.salesbot.messaging.infrastructure.WebhookSignatureVerifier;
import com.salesbot.messaging.infrastructure.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Endpoint registered in the Meta app dashboard as the WhatsApp webhook URL. */
@RestController
@RequestMapping("/webhook")
class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppProperties properties;
    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookService webhookService;
    private final JsonMapper jsonMapper;

    WhatsAppWebhookController(WhatsAppProperties properties,
                              WebhookSignatureVerifier signatureVerifier,
                              WebhookService webhookService,
                              JsonMapper jsonMapper) {
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
        this.jsonMapper = jsonMapper;
    }

    /** One-time verification handshake Meta performs when the webhook URL is saved. */
    @GetMapping
    ResponseEntity<String> verify(@RequestParam("hub.mode") String mode,
                                  @RequestParam("hub.verify_token") String token,
                                  @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && properties.verifyToken().equals(token)) {
            log.info("Webhook verified by Meta");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Webhook verification refused — the Verify token in Meta must equal SALESBOT_WA_VERIFY_TOKEN");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Raw bytes are needed to check the signature before parsing.
     * Always answer 200 quickly for valid calls, otherwise Meta keeps retrying.
     */
    @PostMapping
    ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                 @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected webhook with a bad signature — check SALESBOT_WA_APP_SECRET matches the Meta app");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WebhookPayload payload;
        try {
            payload = jsonMapper.readValue(rawBody, WebhookPayload.class);
        } catch (JacksonException e) {
            log.warn("Unreadable webhook body", e);
            return ResponseEntity.badRequest().build();
        }
        webhookService.handle(payload);
        return ResponseEntity.ok().build();
    }
}
