package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.List;

public final class DeckFactory {
    private DeckFactory() {
    }

    public static List<Card> createDoubleDeck() {
        List<Card> cards = new ArrayList<>();
        for (int deck = 0; deck < 2; deck++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    if (rank != Rank.SMALL_JOKER && rank != Rank.BIG_JOKER) {
                        cards.add(new Card(suit, rank, deck));
                    }
                }
            }
            cards.add(new Card(null, Rank.SMALL_JOKER, deck));
            cards.add(new Card(null, Rank.BIG_JOKER, deck));
        }
        return cards;
    }
}
