package com.chesscard.shengji.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionJpaRepository extends JpaRepository<GameSessionEntity, String> {
}
