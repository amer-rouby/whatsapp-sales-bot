package com.salesbot.messaging;

import java.time.Instant;

/**
 * Published when a customer sends a text message to a business number.
 *
 * @param waMessageId   WhatsApp message id (unique, used to ignore webhook retries)
 * @param phoneNumberId the business number that received the message
 * @param customerWaId  the customer's WhatsApp id (phone number)
 * @param customerName  profile name, may be null
 * @param text          message body
 * @param sentAt        when the customer sent it
 */
public record InboundMessageReceived(
        String waMessageId,
        String phoneNumberId,
        String customerWaId,
        String customerName,
        String text,
        Instant sentAt) {
}
