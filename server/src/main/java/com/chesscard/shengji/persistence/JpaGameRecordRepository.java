package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.GameRecord;
import com.chesscard.shengji.service.GameRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaGameRecordRepository implements GameRecordRepository {
    private final GameRecordJpaRepository jpaRepository;

    public JpaGameRecordRepository(GameRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public GameRecord save(GameRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public Optional<GameRecord> findByGameId(String gameId) {
        return jpaRepository.findByGameId(gameId);
    }

    @Override
    public List<GameRecord> findByPlayerId(String playerId) {
        return jpaRepository.findByPlayerId(playerId);
    }
}