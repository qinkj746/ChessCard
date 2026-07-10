package com.chesscard.shengji.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "game_session")
public class GameSessionEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 32)
    private String phase;

    @Column(nullable = false, length = 32)
    private String levelRank;

    @Column(length = 32)
    private String trumpSuit;

    @Column(length = 32)
    private String banker;

    @Column(nullable = false, length = 32)
    private String currentTurn;

    @Column(nullable = false)
    private int attackerScore;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getLevelRank() {
        return levelRank;
    }

    public void setLevelRank(String levelRank) {
        this.levelRank = levelRank;
    }

    public String getTrumpSuit() {
        return trumpSuit;
    }

    public void setTrumpSuit(String trumpSuit) {
        this.trumpSuit = trumpSuit;
    }

    public String getBanker() {
        return banker;
    }

    public void setBanker(String banker) {
        this.banker = banker;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public int getAttackerScore() {
        return attackerScore;
    }

    public void setAttackerScore(int attackerScore) {
        this.attackerScore = attackerScore;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
