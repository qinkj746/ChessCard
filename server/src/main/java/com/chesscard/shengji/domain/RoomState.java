package com.chesscard.shengji.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class RoomState {
    private String roomId;
    private RoomPhase phase;
    private Map<PlayerSeat, RoomSeat> seats;
    private String ownerPlayerId;
    private String gameId;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public RoomState() {
        this.roomId = UUID.randomUUID().toString();
        this.phase = RoomPhase.WAITING;
        this.seats = new EnumMap<>(PlayerSeat.class);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static RoomState create(String ownerPlayerId) {
        RoomState room = new RoomState();
        room.ownerPlayerId = ownerPlayerId;
        room.seats.put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, ownerPlayerId, Instant.now()));
        room.touch();
        return room;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public RoomPhase getPhase() {
        return phase;
    }

    public void setPhase(RoomPhase phase) {
        this.phase = phase;
    }

    public Map<PlayerSeat, RoomSeat> getSeats() {
        return seats;
    }

    public void setSeats(Map<PlayerSeat, RoomSeat> seats) {
        this.seats = seats;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.version++;
        this.updatedAt = Instant.now();
    }
}