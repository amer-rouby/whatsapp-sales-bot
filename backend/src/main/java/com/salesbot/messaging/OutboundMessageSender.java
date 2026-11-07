package com.salesbot.messaging;

/** Sends a message back to a customer through the channel. */
public interface OutboundMessageSender {

    /**
     * @param phoneNumberId business number to send from
     * @param to            customer WhatsApp id
     * @param text          message body
     */
    void sendText(String phoneNumberId, String to, String text);
}
