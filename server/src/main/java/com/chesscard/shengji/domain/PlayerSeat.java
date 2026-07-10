package com.chesscard.shengji.domain;

public enum PlayerSeat {
    SOUTH,
    WEST,
    NORTH,
    EAST;

    public PlayerSeat next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
