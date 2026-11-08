package com.salesbot.messaging;

/**
 * Asks whether the bot will answer a chat (it does not while the shop has taken the chat over).
 * Declared here and implemented by the conversation module, so messaging never depends on it.
 */
public interface BotAvailability {

    boolean botAnswers(String phoneNumberId, String customerWaId);
}
