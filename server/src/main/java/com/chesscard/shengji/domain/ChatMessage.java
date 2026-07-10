package com.chesscard.shengji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id
    @Column(length = 36)
    private String messageId;

    @Column(nullable = false, length = 36)
    private String roomId;

    @Column(nullable = false, length = 36)
    private String senderPlayerId;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private Instant sentAt;

    protected ChatMessage() {
    }

    public ChatMessage(String messageId, String roomId, String senderPlayerId, String content, Instant sentAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderPlayerId = senderPlayerId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSenderPlayerId() {
        return senderPlayerId;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}