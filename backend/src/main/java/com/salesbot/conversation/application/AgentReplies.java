package com.salesbot.conversation.application;

import com.salesbot.conversation.domain.Conversation;
import com.salesbot.conversation.domain.Message;
import com.salesbot.conversation.infrastructure.ConversationRepository;
import com.salesbot.conversation.infrastructure.MessageRepository;
import com.salesbot.messaging.OutboundMessageSender;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * A person from the shop answers a customer from the dashboard. Replying takes the chat over:
 * the bot stays quiet in it until the shop hands it back.
 */
@Service
public class AgentReplies {

    private static final Logger log = LoggerFactory.getLogger(AgentReplies.class);
    /** WhatsApp's limit for one text message. */
    static final int MAX_LENGTH = 4096;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final OutboundMessageSender sender;
    private final ReplyWindow replyWindow;
    private final TransactionTemplate tx;

    AgentReplies(ConversationRepository conversations, MessageRepository messages,
                 OutboundMessageSender sender, ReplyWindow replyWindow, TransactionTemplate tx) {
        this.replyWindow = replyWindow;
        this.conversations = conversations;
        this.messages = messages;
        this.sender = sender;
        this.tx = tx;
    }

    /** Sends first and stores only what WhatsApp accepted, so the dashboard never shows an undelivered reply. */
    public ConversationQueries.ChatMessage send(UUID conversationId, String text) {
        String body = text == null ? "" : text.strip();
        if (body.isEmpty() || body.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Reply must be 1 to " + MAX_LENGTH + " characters");
        }
        Conversation conversation = tx.execute(status -> find(conversationId));
        if (!replyWindow.isOpen(conversation)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The customer's last message is older than 24 hours; WhatsApp does not allow a free reply");
        }

        try {
            sender.sendText(conversation.getPhoneNumberId(), conversation.getCustomerWaId(), body);
        } catch (RestClientException e) {
            log.warn("Agent reply to conversation {} was not delivered: {}", conversationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WhatsApp did not accept the message", e);
        }

        return tx.execute(status -> {
            Conversation current = find(conversationId);
            current.pauseBot();
            Message saved = messages.saveAndFlush(Message.agentReply(current, body));
            return new ConversationQueries.ChatMessage(saved.getId(), saved.getDirection(), saved.getAuthor(),
                    saved.getBody(), saved.getImageId(), saved.getCreatedAt());
        });
    }

    /** Hands the chat back to the bot (false) or keeps it with the shop (true). */
    public void setBotPaused(UUID conversationId, boolean paused) {
        tx.executeWithoutResult(status -> {
            Conversation conversation = find(conversationId);
            if (paused) {
                conversation.pauseBot();
            } else {
                conversation.resumeBot();
            }
        });
    }

    private Conversation find(UUID id) {
        return conversations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation " + id + " not found"));
    }
}
