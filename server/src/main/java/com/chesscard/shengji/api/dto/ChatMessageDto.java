package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.ChatMessage;

import java.time.Instant;

public record ChatMessageDto(
        String messageId,
        String roomId,
        String senderPlayerId,
        String content,
        Instant sentAt
) {
    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getMessageId(),
                message.getRoomId(),
                message.getSenderPlayerId(),
                message.getContent(),
                message.getSentAt()
        );
    }
}