package com.chesscard.shengji.domain;

public record Card(Suit suit, Rank rank, int deckIndex) {
    public boolean isJoker() {
        return rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER;
    }
}
