package com.salesbot.conversation.domain;

import com.salesbot.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private Direction direction;

    /** Null for our own replies until WhatsApp returns an id. */
    @Column(name = "wa_message_id", unique = true)
    private String waMessageId;

    @Column(name = "body", nullable = false)
    private String body;

    protected Message() {
    }

    private Message(Conversation conversation, Direction direction, String waMessageId, String body) {
        this.conversation = conversation;
        this.direction = direction;
        this.waMessageId = waMessageId;
        this.body = body;
    }

    public static Message inbound(Conversation conversation, String waMessageId, String body) {
        return new Message(conversation, Direction.INBOUND, waMessageId, body);
    }

    public static Message outbound(Conversation conversation, String body) {
        return new Message(conversation, Direction.OUTBOUND, null, body);
    }

    public Direction getDirection() {
        return direction;
    }

    public String getBody() {
        return body;
    }

    public enum Direction { INBOUND, OUTBOUND }
}
