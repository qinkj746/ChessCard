package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GameRecord;

import java.util.List;
import java.util.Optional;

public interface GameRecordRepository {
    GameRecord save(GameRecord record);

    Optional<GameRecord> findByGameId(String gameId);

    List<GameRecord> findByPlayerId(String playerId);
}