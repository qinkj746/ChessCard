package com.chesscard.shengji.testutil;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.List;

public final class CardFixtures {
    private CardFixtures() {
    }

    public static Card card(Suit suit, Rank rank, int deckIndex) {
        return new Card(suit, rank, deckIndex);
    }

    public static List<Card> pair(Suit suit, Rank rank) {
        return List.of(
                card(suit, rank, 0),
                card(suit, rank, 1)
        );
    }

    public static List<Card> tractor(Suit suit, Rank... ranks) {
        List<Card> cards = new ArrayList<>();
        for (Rank rank : ranks) {
            cards.addAll(pair(suit, rank));
        }
        return List.copyOf(cards);
    }
}