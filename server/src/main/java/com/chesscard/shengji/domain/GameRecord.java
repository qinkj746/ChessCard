package com.chesscard.shengji.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(name = "game_record")
public class GameRecord {
    @Id
    @Column(length = 36)
    private String recordId;

    @Column(nullable = false, unique = true, length = 36)
    private String gameId;

    @Column(length = 36)
    private String roomId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    @ElementCollection
    @CollectionTable(name = "game_record_player", joinColumns = @JoinColumn(name = "record_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "seat", length = 16)
    @Column(name = "player_id", length = 36)
    private Map<PlayerSeat, String> players = new EnumMap<>(PlayerSeat.class);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Team winningTeam;

    @Column(nullable = false)
    private int attackerScore;

    @Column(nullable = false)
    private int levelDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Rank nextLevelRank;

    @Column(nullable = false)
    private boolean completed;

    protected GameRecord() {
    }

    public GameRecord(
            String recordId,
            String gameId,
            String roomId,
            Instant startedAt,
            Instant finishedAt,
            Map<PlayerSeat, String> players,
            Team winningTeam,
            int attackerScore,
            int levelDelta,
            Rank nextLevelRank,
            boolean completed
    ) {
        this.recordId = recordId;
        this.gameId = gameId;
        this.roomId = roomId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.players = new EnumMap<>(PlayerSeat.class);
        this.players.putAll(players);
        this.winningTeam = winningTeam;
        this.attackerScore = attackerScore;
        this.levelDelta = levelDelta;
        this.nextLevelRank = nextLevelRank;
        this.completed = completed;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getGameId() {
        return gameId;
    }

    public String getRoomId() {
        return roomId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Map<PlayerSeat, String> getPlayers() {
        return players;
    }

    public Team getWinningTeam() {
        return winningTeam;
    }

    public int getAttackerScore() {
        return attackerScore;
    }

    public int getLevelDelta() {
        return levelDelta;
    }

    public Rank getNextLevelRank() {
        return nextLevelRank;
    }

    public boolean isCompleted() {
        return completed;
    }
}