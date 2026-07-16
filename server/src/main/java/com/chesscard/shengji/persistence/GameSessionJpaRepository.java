package com.chesscard.shengji.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameSessionJpaRepository extends JpaRepository<GameSessionEntity, String> {
    List<GameSessionEntity> findByRoomIdOrderByUpdatedAtDesc(String roomId);
}
