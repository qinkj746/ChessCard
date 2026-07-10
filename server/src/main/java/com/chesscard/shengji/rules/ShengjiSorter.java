package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ShengjiSorter {
    private ShengjiSorter() {
    }

    public static List<Card> sortHand(List<Card> hand, Rank levelRank, Suit trumpSuit) {
        if (hand == null) {
            throw new IllegalArgumentException("手牌不能为空");
        }
        if (hand.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("手牌不能包含空牌");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        List<Card> copy = new ArrayList<>(hand);
        copy.sort(Comparator.comparingInt(card -> sortWeight(card, levelRank, trumpSuit)));
        return copy;
    }

    private static int sortWeight(Card card, Rank levelRank, Suit trumpSuit) {
        if (card.rank() == Rank.BIG_JOKER) {
            return 0;
        }
        if (card.rank() == Rank.SMALL_JOKER) {
            return 1;
        }
        if (card.suit() == trumpSuit && card.rank() == levelRank) {
            return 10;
        }
        if (card.rank() == levelRank) {
            return 20 + suitOrder(card.suit());
        }
        if (card.suit() == trumpSuit && card.rank() == Rank.TWO) {
            return 30;
        }
        if (card.rank() == Rank.TWO) {
            return 40 + suitOrder(card.suit());
        }
        if (card.suit() == trumpSuit) {
            return 50 + rankDescending(card.rank());
        }
        return 100 + suitOrder(card.suit()) * 20 + rankDescending(card.rank());
    }

    private static int suitOrder(Suit suit) {
        return switch (suit) {
            case SPADE -> 0;
            case HEART -> 1;
            case CLUB -> 2;
            case DIAMOND -> 3;
        };
    }

    private static int rankDescending(Rank rank) {
        return switch (rank) {
            case ACE -> 0;
            case KING -> 1;
            case QUEEN -> 2;
            case JACK -> 3;
            case TEN -> 4;
            case NINE -> 5;
            case EIGHT -> 6;
            case SEVEN -> 7;
            case SIX -> 8;
            case FIVE -> 9;
            case FOUR -> 10;
            case THREE -> 11;
            case TWO, SMALL_JOKER, BIG_JOKER -> 99;
        };
    }
}
