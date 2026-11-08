package com.salesbot.conversation.application;

import com.salesbot.conversation.domain.Conversation;
import com.salesbot.conversation.domain.Message;
import com.salesbot.conversation.infrastructure.MessageRepository;
import com.salesbot.messaging.SimulatorChannel;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * WhatsApp only delivers free-form messages within 24 hours of the customer's last message
 * (after that a pre-approved template is required). Simulator chats never reach WhatsApp.
 */
@Component
class ReplyWindow {

    static final Duration LENGTH = Duration.ofHours(24);

    private final MessageRepository messages;

    ReplyWindow(MessageRepository messages) {
        this.messages = messages;
    }

    boolean isOpen(Conversation conversation) {
        if (SimulatorChannel.PHONE_NUMBER_ID.equals(conversation.getPhoneNumberId())) {
            return true;
        }
        return messages.findFirstByConversation_IdAndAuthorOrderByCreatedAtDesc(conversation.getId(), Message.Author.CUSTOMER)
                .map(last -> last.getCreatedAt().isAfter(Instant.now().minus(LENGTH)))
                .orElse(false);
    }
}
