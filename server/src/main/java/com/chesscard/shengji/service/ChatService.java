package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameNotFoundException;
import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.websocket.RoomEventType;
import com.chesscard.shengji.domain.ChatMessage;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private static final int MAX_MESSAGE_LENGTH = 200;

    private final ChatMessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomEventPublisher eventPublisher;

    public ChatService(
            ChatMessageRepository messageRepository,
            RoomRepository roomRepository,
            RoomEventPublisher eventPublisher
    ) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
    }

    public ChatMessage sendMessage(String roomId, String senderPlayerId, String content) {
        RoomState room = getRoom(roomId);
        if (senderPlayerId == null || senderPlayerId.isBlank()) {
            throw new IllegalArgumentException("playerId is required");
        }
        boolean member = room.getSeats().values().stream()
                .map(RoomSeat::getPlayerId)
                .anyMatch(senderPlayerId::equals);
        if (!member) {
            throw new PermissionDeniedException("player is not in room");
        }
        String normalizedContent = normalizeContent(content);
        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                room.getRoomId(),
                senderPlayerId,
                normalizedContent,
                Instant.now()
        );
        ChatMessage saved = messageRepository.save(message);
        eventPublisher.publish(new RoomEvent(RoomEventType.CHAT_MESSAGE, room.getRoomId(), null, null, saved));
        return saved;
    }

    public List<ChatMessage> listMessages(String roomId) {
        getRoom(roomId);
        return messageRepository.findByRoomId(roomId);
    }

    private RoomState getRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId is required");
        }
        return roomRepository.find(roomId)
                .orElseThrow(() -> new GameNotFoundException("room not found"));
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("message content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message content is too long");
        }
        return normalized;
    }
}