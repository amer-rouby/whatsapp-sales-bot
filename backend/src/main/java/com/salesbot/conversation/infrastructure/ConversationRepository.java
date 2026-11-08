package com.salesbot.conversation.infrastructure;

import com.salesbot.conversation.domain.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** One row per conversation with its latest message, newest activity first (one query, no N+1). */
    interface SummaryRow {
        UUID getId();

        String getCustomerName();

        String getCustomerWaId();

        String getPhoneNumberId();

        String getLastBody();

        String getLastDirection();

        Instant getLastAt();

        long getMessageCount();
    }

    @Query(nativeQuery = true, value = """
            SELECT c.id AS id, c.customer_name AS customerName, c.customer_wa_id AS customerWaId,
                   c.phone_number_id AS phoneNumberId,
                   last.body AS lastBody, last.direction AS lastDirection, last.created_at AS lastAt,
                   cnt.n AS messageCount
            FROM conversations c
            JOIN LATERAL (SELECT body, direction, created_at FROM messages m
                          WHERE m.conversation_id = c.id ORDER BY m.created_at DESC LIMIT 1) last ON TRUE
            JOIN LATERAL (SELECT count(*) AS n FROM messages m WHERE m.conversation_id = c.id) cnt ON TRUE
            ORDER BY last.created_at DESC
            LIMIT :limit
            """)
    List<SummaryRow> findSummaries(@Param("limit") int limit);

    Optional<Conversation> findByPhoneNumberIdAndCustomerWaId(String phoneNumberId, String customerWaId);

    /**
     * Race-safe "create if missing": when two messages from a new customer arrive together,
     * the database lets exactly one insert win and the other becomes a no-op
     * (it waits for the winner to commit, then skips).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO conversations (id, phone_number_id, customer_wa_id, customer_name, created_at, updated_at)
            VALUES (gen_random_uuid(), :phoneNumberId, :customerWaId, :customerName, now(), now())
            ON CONFLICT (phone_number_id, customer_wa_id) DO NOTHING
            """)
    int insertIfAbsent(@Param("phoneNumberId") String phoneNumberId,
                       @Param("customerWaId") String customerWaId,
                       @Param("customerName") String customerName);
}
