package com.salesbot.conversation.application;

import com.salesbot.conversation.domain.Message;
import com.salesbot.conversation.infrastructure.ConversationRepository;
import com.salesbot.conversation.infrastructure.MessageRepository;
import com.salesbot.messaging.SimulatorChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Read side for the dashboard: who talked to the bot and what was said. */
@Service
@Transactional(readOnly = true)
public class ConversationQueries {

    /** Enough for a small shop's inbox; paging comes with multi-store. */
    static final int MAX_CONVERSATIONS = 200;
    /** The latest messages shown for one chat. */
    static final int MAX_MESSAGES = 500;

    private final ConversationRepository conversations;
    private final MessageRepository messages;

    ConversationQueries(ConversationRepository conversations, MessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    public List<ConversationSummary> list() {
        return conversations.findSummaries(MAX_CONVERSATIONS).stream()
                .map(r -> new ConversationSummary(r.getId(), r.getCustomerName(), r.getCustomerWaId(),
                        SimulatorChannel.PHONE_NUMBER_ID.equals(r.getPhoneNumberId()),
                        r.getLastBody(), Message.Direction.valueOf(r.getLastDirection()), r.getLastAt(),
                        r.getMessageCount()))
                .toList();
    }

    public ConversationDetail get(UUID id) {
        var conversation = conversations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation " + id + " not found"));
        List<Message> newestFirst = messages.findByConversation_IdOrderByCreatedAtDesc(id, Limit.of(MAX_MESSAGES));
        List<ChatMessage> lines = new ArrayList<>(newestFirst.size());
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            Message m = newestFirst.get(i);
            lines.add(new ChatMessage(m.getId(), m.getDirection(), m.getBody(), m.getCreatedAt()));
        }
        return new ConversationDetail(conversation.getId(), conversation.getCustomerName(), conversation.getCustomerWaId(),
                SimulatorChannel.PHONE_NUMBER_ID.equals(conversation.getPhoneNumberId()), lines);
    }

    /** @param simulated a chat from the dashboard simulator, not a real WhatsApp customer */
    public record ConversationSummary(UUID id, String customerName, String customerWaId, boolean simulated, String lastMessage,
                          Message.Direction lastDirection, Instant lastMessageAt, long messageCount) {
    }

    public record ConversationDetail(UUID id, String customerName, String customerWaId, boolean simulated, List<ChatMessage> messages) {
    }

    public record ChatMessage(UUID id, Message.Direction direction, String body, Instant sentAt) {
    }
}
