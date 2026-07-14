package com.chesscard.shengji.domain;

import java.time.Instant;

public class RoomSeat {
    private PlayerSeat seat;
    private String playerId;
    private Instant joinedAt;
    private boolean bot;

    public RoomSeat() {
    }

    public RoomSeat(PlayerSeat seat, String playerId, Instant joinedAt) {
        this.seat = seat;
        this.playerId = playerId;
        this.joinedAt = joinedAt;
        this.bot = false;
    }

    public static RoomSeat bot(PlayerSeat seat, Instant joinedAt) {
        RoomSeat roomSeat = new RoomSeat(seat, null, joinedAt);
        roomSeat.bot = true;
        return roomSeat;
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

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }
}
