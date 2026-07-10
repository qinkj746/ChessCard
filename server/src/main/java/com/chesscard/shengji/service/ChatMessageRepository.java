package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);

    List<ChatMessage> findByRoomId(String roomId);
}