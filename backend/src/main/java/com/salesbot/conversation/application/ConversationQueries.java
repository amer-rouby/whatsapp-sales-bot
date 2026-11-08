package com.salesbot.conversation.application;

import com.salesbot.conversation.CustomerMessages;
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
public class ConversationQueries implements CustomerMessages {

    /** Enough for a small shop's inbox; paging comes with multi-store. */
    static final int MAX_CONVERSATIONS = 200;
    /** The latest messages shown for one chat. */
    static final int MAX_MESSAGES = 500;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ReplyWindow replyWindow;

    ConversationQueries(ConversationRepository conversations, MessageRepository messages, ReplyWindow replyWindow) {
        this.conversations = conversations;
        this.messages = messages;
        this.replyWindow = replyWindow;
    }

    public List<ConversationSummary> list() {
        return conversations.findSummaries(MAX_CONVERSATIONS).stream()
                .map(r -> new ConversationSummary(r.getId(), r.getCustomerName(), r.getCustomerWaId(),
                        isSimulated(r.getPhoneNumberId()), r.getBotPaused(),
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
            lines.add(ChatMessage.of(newestFirst.get(i)));
        }
        return new ConversationDetail(conversation.getId(), conversation.getCustomerName(), conversation.getCustomerWaId(),
                isSimulated(conversation.getPhoneNumberId()), conversation.isBotPaused(),
                replyWindow.isOpen(conversation), lines);
    }

    @Override
    public List<CustomerMessage> since(Instant after, int limit) {
        return messages.findCustomerMessagesAfter(after, limit).stream()
                .map(r -> new CustomerMessage(r.getId(), r.getConversationId(), r.getCustomerName(), r.getCustomerWaId(),
                        isSimulated(r.getPhoneNumberId()), r.getBotPaused(), r.getBody(), r.getCreatedAt()))
                .toList();
    }

    private static boolean isSimulated(String phoneNumberId) {
        return SimulatorChannel.PHONE_NUMBER_ID.equals(phoneNumberId);
    }

    /**
     * @param simulated a chat from the dashboard simulator, not a real WhatsApp customer
     * @param botPaused the shop took the chat over; the bot stays quiet until it is handed back
     */
    public record ConversationSummary(UUID id, String customerName, String customerWaId, boolean simulated, boolean botPaused,
                                      String lastMessage, Message.Direction lastDirection, Instant lastMessageAt,
                                      long messageCount) {
    }

    /** @param canReply WhatsApp still accepts a free reply (customer wrote within 24 hours, or a simulator chat) */
    public record ConversationDetail(UUID id, String customerName, String customerWaId, boolean simulated,
                                     boolean botPaused, boolean canReply, List<ChatMessage> messages) {
    }

    public record ChatMessage(UUID id, Message.Direction direction, Message.Author author, String body, Instant sentAt) {

        static ChatMessage of(Message m) {
            return new ChatMessage(m.getId(), m.getDirection(), m.getAuthor(), m.getBody(), m.getCreatedAt());
        }
    }
}
