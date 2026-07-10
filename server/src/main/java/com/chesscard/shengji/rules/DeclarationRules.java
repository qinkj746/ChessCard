package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DeclarationRules {
    private DeclarationRules() {
    }

    public static List<Suit> availableSuits(List<Card> hand, Rank levelRank) {
        if (hand == null) {
            throw new IllegalArgumentException("手牌不能为空");
        }
        if (hand.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("手牌不能包含空牌");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        boolean hasSmallJoker = hand.stream().anyMatch(card -> card.rank() == Rank.SMALL_JOKER);
        boolean hasBigJoker = hand.stream().anyMatch(card -> card.rank() == Rank.BIG_JOKER);
        List<Suit> result = new ArrayList<>();
        if (hasSmallJoker) {
            addIfHasLevelCard(result, hand, levelRank, Suit.SPADE);
            addIfHasLevelCard(result, hand, levelRank, Suit.CLUB);
        }
        if (hasBigJoker) {
            addIfHasLevelCard(result, hand, levelRank, Suit.HEART);
            addIfHasLevelCard(result, hand, levelRank, Suit.DIAMOND);
        }
        return result;
    }

    private static void addIfHasLevelCard(List<Suit> result, List<Card> hand, Rank levelRank, Suit suit) {
        boolean present = hand.stream().anyMatch(card -> card.suit() == suit && card.rank() == levelRank);
        if (present) {
            result.add(suit);
        }
    }
}
