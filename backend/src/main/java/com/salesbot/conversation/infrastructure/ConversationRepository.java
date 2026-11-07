package com.salesbot.conversation.infrastructure;

import com.salesbot.conversation.domain.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

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
