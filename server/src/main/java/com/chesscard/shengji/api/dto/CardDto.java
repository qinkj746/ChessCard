package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

public record CardDto(Suit suit, Rank rank, int deckIndex) {
    public static CardDto from(Card card) {
        return new CardDto(card.suit(), card.rank(), card.deckIndex());
    }

    public Card toCard() {
        return new Card(suit, rank, deckIndex);
    }
}
