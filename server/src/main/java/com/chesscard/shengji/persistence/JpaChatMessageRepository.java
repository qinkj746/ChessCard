package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.ChatMessage;
import com.chesscard.shengji.service.ChatMessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaChatMessageRepository implements ChatMessageRepository {
    private final ChatMessageJpaRepository jpaRepository;

    public JpaChatMessageRepository(ChatMessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        return jpaRepository.save(message);
    }

    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        return jpaRepository.findByRoomIdOrderBySentAtAsc(roomId);
    }
}