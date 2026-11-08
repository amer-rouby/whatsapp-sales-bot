package com.salesbot.conversation.infrastructure;

import com.salesbot.conversation.domain.Conversation;
import com.salesbot.conversation.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    boolean existsByWaMessageId(String waMessageId);

    /** The latest messages of a conversation, newest first (the dashboard reverses them). */
    List<Message> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Limit limit);

    /** Newest first; callers reverse it to get chronological history. */
    List<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Limit limit);

    /** A customer message with its chat, for notifications. */
    interface CustomerMessageRow {
        UUID getId();

        UUID getConversationId();

        String getCustomerName();

        String getCustomerWaId();

        String getPhoneNumberId();

        boolean getBotPaused();

        String getBody();

        Instant getCreatedAt();
    }

    @Query(nativeQuery = true, value = """
            SELECT m.id AS id, c.id AS conversationId, c.customer_name AS customerName,
                   c.customer_wa_id AS customerWaId, c.phone_number_id AS phoneNumberId,
                   c.bot_paused AS botPaused, m.body AS body, m.created_at AS createdAt
            FROM messages m JOIN conversations c ON c.id = m.conversation_id
            WHERE m.author = 'CUSTOMER' AND m.created_at > :after
            ORDER BY m.created_at DESC
            LIMIT :limit
            """)
    List<CustomerMessageRow> findCustomerMessagesAfter(@Param("after") Instant after, @Param("limit") int limit);

    /** The customer's latest message: WhatsApp only allows free-form replies within 24 hours of it. */
    Optional<Message> findFirstByConversation_IdAndAuthorOrderByCreatedAtDesc(UUID conversationId, Message.Author author);

    /**
     * Stores an inbound message unless its WhatsApp id was already stored.
     * This single statement IS the duplicate check: concurrent webhook retries
     * cannot both get 1 back, so the customer is answered exactly once.
     *
     * @return 1 if stored, 0 if it was a duplicate
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO messages (id, conversation_id, direction, author, wa_message_id, body, created_at, updated_at)
            VALUES (gen_random_uuid(), :conversationId, 'INBOUND', 'CUSTOMER', :waMessageId, :body, now(), now())
            ON CONFLICT (wa_message_id) DO NOTHING
            """)
    int insertInboundIfNew(@Param("conversationId") UUID conversationId,
                           @Param("waMessageId") String waMessageId,
                           @Param("body") String body);
}
