package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);
}