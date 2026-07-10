package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.GameRecord;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public record GameRecordDto(
        String recordId,
        String gameId,
        String roomId,
        Instant startedAt,
        Instant finishedAt,
        Map<String, String> players,
        String winningTeam,
        int attackerScore,
        int levelDelta,
        String nextLevelRank,
        boolean completed
) {
    public static GameRecordDto from(GameRecord record) {
        return new GameRecordDto(
                record.getRecordId(),
                record.getGameId(),
                record.getRoomId(),
                record.getStartedAt(),
                record.getFinishedAt(),
                record.getPlayers().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)),
                record.getWinningTeam().name(),
                record.getAttackerScore(),
                record.getLevelDelta(),
                record.getNextLevelRank().name(),
                record.isCompleted()
        );
    }
}