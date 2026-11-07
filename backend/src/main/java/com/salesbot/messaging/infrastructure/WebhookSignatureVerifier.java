package com.salesbot.messaging.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks Meta's {@code X-Hub-Signature-256} header: HMAC-SHA256 of the raw body
 * keyed with the app secret. Rejects forged webhook calls.
 */
@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
    private static final String PREFIX = "sha256=";

    private final WhatsAppProperties properties;

    WebhookSignatureVerifier(WhatsAppProperties properties) {
        this.properties = properties;
        if (!properties.verifySignature()) {
            log.warn("Webhook signature verification is DISABLED — never run like this in production");
        }
    }

    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (!properties.verifySignature()) {
            return true;
        }
        String secret = properties.appSecret();
        if (secret == null || secret.isBlank()) {
            log.error("salesbot.whatsapp.app-secret is not set; rejecting webhook");
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        byte[] expected = hmacSha256(secret, rawBody);
        byte[] received;
        try {
            received = HexFormat.of().parseHex(signatureHeader.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        // constant-time comparison
        return MessageDigest.isEqual(expected, received);
    }

    private static byte[] hmacSha256(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
