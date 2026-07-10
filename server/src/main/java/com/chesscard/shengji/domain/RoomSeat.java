package com.chesscard.shengji.domain;

import java.time.Instant;

public class RoomSeat {
    private PlayerSeat seat;
    private String playerId;
    private Instant joinedAt;

    public RoomSeat() {
    }

    public RoomSeat(PlayerSeat seat, String playerId, Instant joinedAt) {
        this.seat = seat;
        this.playerId = playerId;
        this.joinedAt = joinedAt;
    }

    public PlayerSeat getSeat() {
        return seat;
    }

    public void setSeat(PlayerSeat seat) {
        this.seat = seat;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
