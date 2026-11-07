package com.salesbot.conversation.application;

import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.AiUnavailableException;
import com.salesbot.ai.ReplyRequest;
import com.salesbot.catalog.CatalogQuery;
import com.salesbot.conversation.domain.Conversation;
import com.salesbot.conversation.domain.Message;
import com.salesbot.conversation.infrastructure.ConversationRepository;
import com.salesbot.conversation.infrastructure.MessageRepository;
import com.salesbot.messaging.InboundMessageReceived;
import com.salesbot.messaging.OutboundMessageSender;
import com.salesbot.orders.OrderPlacement;
import com.salesbot.orders.PlaceOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The reply loop for every inbound customer message:
 * store it (tx 1) → ask the AI (no transaction: it can take seconds) → store the reply (tx 2) → send.
 */
@Service
class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    /** How many earlier messages the AI sees. */
    private static final int HISTORY_SIZE = 20;
    /** What the customer gets when the AI is down or rate-limited — never silence. */
    static final String FALLBACK_REPLY = "أهلاً بيك 🙏 ثواني وحد من فريقنا هيرد عليك.";

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AiReplyProvider aiReplyProvider;
    private final OutboundMessageSender sender;
    private final CatalogQuery catalog;
    private final OrderPlacement orderPlacement;
    private final TransactionTemplate tx;

    ConversationService(ConversationRepository conversations,
                        MessageRepository messages,
                        AiReplyProvider aiReplyProvider,
                        OutboundMessageSender sender,
                        CatalogQuery catalog,
                        OrderPlacement orderPlacement,
                        TransactionTemplate tx) {
        this.conversations = conversations;
        this.messages = messages;
        this.aiReplyProvider = aiReplyProvider;
        this.sender = sender;
        this.catalog = catalog;
        this.orderPlacement = orderPlacement;
        this.tx = tx;
    }

    /** Runs on a background thread after the webhook transaction commits. */
    @Async
    @TransactionalEventListener
    void on(InboundMessageReceived event) {
        Stored stored = tx.execute(status -> storeInbound(event));
        if (stored == null) {
            log.info("Duplicate delivery of {}, ignored", event.waMessageId());
            return;
        }

        AiReply reply = generateReply(stored, event.text());

        tx.executeWithoutResult(status -> messages.save(
                Message.outbound(conversations.getReferenceById(stored.conversationId()), reply.text())));
        if (reply.order() != null) {
            recordOrder(stored, event, reply.order());
        }
        sender.sendText(event.phoneNumberId(), event.customerWaId(), reply.text());
    }

    /** Never blocks the customer's reply: a failed order is logged for a human to pick up. */
    private void recordOrder(Stored stored, InboundMessageReceived event, AiReply.OrderDraft draft) {
        try {
            orderPlacement.place(new PlaceOrder(
                    stored.conversationId(),
                    event.customerWaId(),
                    draft.customerName() != null ? draft.customerName() : stored.customerName(),
                    draft.address(),
                    draft.items().stream()
                            .map(i -> new PlaceOrder.Item(i.productName(), i.size(), i.color(), i.quantity()))
                            .toList()));
        } catch (RuntimeException e) {
            log.error("Could not record order for conversation {} — check the chat", stored.conversationId(), e);
        }
    }

    /** @return null when this WhatsApp message was already handled (webhook retry) */
    private Stored storeInbound(InboundMessageReceived event) {
        // Both writes are atomic in the database, so concurrent deliveries can't race:
        // 1) exactly one conversation per (business number, customer)
        conversations.insertIfAbsent(event.phoneNumberId(), event.customerWaId(), event.customerName());
        Conversation conversation = conversations
                .findByPhoneNumberIdAndCustomerWaId(event.phoneNumberId(), event.customerWaId())
                .orElseThrow();
        conversation.updateCustomerName(event.customerName());

        // history is read before storing the new message so it holds earlier turns only
        List<ReplyRequest.Turn> history = loadHistory(conversation);

        // 2) only the delivery that actually stores the message gets to answer
        if (messages.insertInboundIfNew(conversation.getId(), event.waMessageId(), event.text()) == 0) {
            return null;
        }
        return new Stored(conversation.getId(), conversation.getCustomerName(), history);
    }

    private AiReply generateReply(Stored stored, String customerText) {
        try {
            return aiReplyProvider.reply(new ReplyRequest(
                    stored.customerName(), customerText, stored.history(), currentProducts()));
        } catch (AiUnavailableException e) {
            log.warn("AI unavailable, sending fallback reply: {}", e.getMessage());
            return AiReply.text(FALLBACK_REPLY);
        }
    }

    /** Read fresh every message, so a product edited in the dashboard is used on the very next reply. */
    private List<ReplyRequest.ProductFact> currentProducts() {
        return catalog.products().stream()
                .map(p -> new ReplyRequest.ProductFact(p.name(), p.category(), p.description(), p.price(),
                        p.currency(), p.sizes(), p.colors(), p.inStock()))
                .toList();
    }

    private List<ReplyRequest.Turn> loadHistory(Conversation conversation) {
        List<Message> newestFirst = messages.findByConversationOrderByCreatedAtDesc(
                conversation, Limit.of(HISTORY_SIZE));
        List<ReplyRequest.Turn> turns = new ArrayList<>(newestFirst.size());
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            Message m = newestFirst.get(i);
            ReplyRequest.Role role = m.getDirection() == Message.Direction.INBOUND
                    ? ReplyRequest.Role.CUSTOMER
                    : ReplyRequest.Role.BOT;
            turns.add(new ReplyRequest.Turn(role, m.getBody()));
        }
        return turns;
    }

    /** What phase 1 hands to the rest of the loop (plain data, no managed entities). */
    private record Stored(UUID conversationId, String customerName, List<ReplyRequest.Turn> history) {
    }
}
