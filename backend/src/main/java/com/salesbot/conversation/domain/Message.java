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

    @Enumerated(EnumType.STRING)
    @Column(name = "author", nullable = false)
    private Author author;

    /** Null for our own replies until WhatsApp returns an id. */
    @Column(name = "wa_message_id", unique = true)
    private String waMessageId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "image_id")
    private java.util.UUID imageId;

    protected Message() {
    }

    private Message(Conversation conversation, Direction direction, Author author, String waMessageId, String body) {
        this.conversation = conversation;
        this.direction = direction;
        this.author = author;
        this.waMessageId = waMessageId;
        this.body = body;
    }

    public static Message inbound(Conversation conversation, String waMessageId, String body) {
        return new Message(conversation, Direction.INBOUND, Author.CUSTOMER, waMessageId, body);
    }

    /** The bot's reply. */
    public static Message outbound(Conversation conversation, String body) {
        return new Message(conversation, Direction.OUTBOUND, Author.BOT, null, body);
    }

    /** A reply typed by a person from the shop in the dashboard. */
    public static Message agentReply(Conversation conversation, String body) {
        return new Message(conversation, Direction.OUTBOUND, Author.AGENT, null, body);
    }

    /**
     * A product photo the bot sent. The body stays readable on its own ("[صورة: «name»]") so the AI history
     * knows the photo was already sent.
     */
    public static Message botPhoto(Conversation conversation, String body, java.util.UUID imageId) {
        Message message = new Message(conversation, Direction.OUTBOUND, Author.BOT, null, body);
        message.imageId = imageId;
        return message;
    }

    /** The catalog photo this message shows, or null for a text message. */
    public java.util.UUID getImageId() {
        return imageId;
    }

    public Direction getDirection() {
        return direction;
    }

    public Author getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public enum Direction { INBOUND, OUTBOUND }

    /** Who wrote the message: the customer, the bot, or a person from the shop. */
    public enum Author { CUSTOMER, BOT, AGENT }
}
