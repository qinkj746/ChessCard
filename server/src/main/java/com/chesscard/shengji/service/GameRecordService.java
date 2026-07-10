package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameRecordNotFoundException;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameRecord;
import com.chesscard.shengji.domain.GameState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GameRecordService {
    private final GameRecordRepository repository;

    public GameRecordService(GameRecordRepository repository) {
        this.repository = repository;
    }

    public GameRecord recordFinishedGame(GameState game) {
        if (game.getPhase() != GamePhase.FINISHED) {
            throw new IllegalArgumentException("game is not finished");
        }
        return repository.findByGameId(game.getId()).orElseGet(() -> {
            Instant finishedAt = Instant.now();
            GameRecord record = new GameRecord(
                    UUID.randomUUID().toString(),
                    game.getId(),
                    game.getRoomId(),
                    finishedAt,
                    finishedAt,
                    game.getSeatOwners(),
                    game.getWinningTeam(),
                    game.getAttackerScore(),
                    game.getLevelDelta(),
                    game.getNextLevelRank(),
                    game.isCompleted()
            );
            return repository.save(record);
        });
    }

    public List<GameRecord> findRecordsForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId is required");
        }
        return repository.findByPlayerId(playerId);
    }

    public GameRecord getRecordForGame(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("gameId is required");
        }
        return repository.findByGameId(gameId)
                .orElseThrow(() -> new GameRecordNotFoundException("game record not found"));
    }
}