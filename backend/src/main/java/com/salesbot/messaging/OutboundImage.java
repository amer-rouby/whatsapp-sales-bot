package com.salesbot.messaging;

/**
 * A photo to send to a customer.
 *
 * @param cacheKey    stable id of these exact bytes (e.g. "product-image:{id}"), so WhatsApp uploads are reused
 * @param contentType image/jpeg or image/png (what WhatsApp image messages accept)
 * @param caption     text under the photo, may be null
 */
public record OutboundImage(String cacheKey, String contentType, byte[] data, String caption) {
}
