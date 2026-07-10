package com.chesscard.shengji.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "room_state")
public class RoomStateEntity {
    @Id
    @Column(length = 36)
    private String roomId;

    @Column(nullable = false, length = 32)
    private String phase;

    @Column(nullable = false, length = 36)
    private String ownerPlayerId;

    @Column(length = 36)
    private String gameId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(String ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
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
