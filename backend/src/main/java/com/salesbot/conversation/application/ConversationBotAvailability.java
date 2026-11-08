package com.salesbot.conversation.application;

import com.salesbot.conversation.infrastructure.ConversationRepository;
import com.salesbot.messaging.BotAvailability;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The bot answers every chat except the ones the shop has taken over (a new chat is answered). */
@Component
class ConversationBotAvailability implements BotAvailability {

    private final ConversationRepository conversations;

    ConversationBotAvailability(ConversationRepository conversations) {
        this.conversations = conversations;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean botAnswers(String phoneNumberId, String customerWaId) {
        return conversations.findByPhoneNumberIdAndCustomerWaId(phoneNumberId, customerWaId)
                .map(conversation -> !conversation.isBotPaused())
                .orElse(true);
    }
}
