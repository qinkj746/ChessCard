package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrickRules {
    private TrickRules() {
    }

    public enum PlayType {
        SINGLE,
        PAIR,
        TRACTOR
    }

    public record PlayPattern(PlayType type, int cardCount, Suit leadSuit, boolean trump, int highWeight) {
    }

    public static PlayPattern analyze(List<Card> cards, Rank levelRank, Suit trumpSuit) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("至少选择 1 张牌");
        }
        if (cards.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("牌不能为空");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        if (cards.stream().distinct().count() != cards.size()) {
            throw new IllegalArgumentException("不能重复使用同一张牌");
        }
        if (cards.size() == 1) {
            Card card = cards.get(0);
            return new PlayPattern(PlayType.SINGLE, 1, effectiveSuit(card, levelRank, trumpSuit),
                    isTrump(card, levelRank, trumpSuit), rankWeight(card, levelRank, trumpSuit));
        }
        if (cards.size() == 2 && sameCardFace(cards.get(0), cards.get(1))) {
            Card card = cards.get(0);
            return new PlayPattern(PlayType.PAIR, 2, effectiveSuit(card, levelRank, trumpSuit),
                    isTrump(card, levelRank, trumpSuit), rankWeight(card, levelRank, trumpSuit));
        }
        if (cards.size() >= 4 && cards.size() % 2 == 0) {
            return analyzeTractor(cards, levelRank, trumpSuit);
        }
        throw new IllegalArgumentException("暂只支持单张、对子和基础拖拉机");
    }

    public static boolean followsLead(List<Card> playedCards, List<Card> handBeforePlay, PlayPattern leadPattern,
                                      Rank levelRank, Suit trumpSuit) {
        if (playedCards == null) {
            throw new IllegalArgumentException("出牌列表不能为空");
        }
        if (playedCards.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("出牌不能为空");
        }
        if (handBeforePlay == null) {
            throw new IllegalArgumentException("出牌前手牌不能为空");
        }
        if (handBeforePlay.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("出牌前手牌不能包含空牌");
        }
        if (leadPattern == null) {
            throw new IllegalArgumentException("首家牌型不能为空");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        long playableLeadSuitCount = handBeforePlay.stream()
                .filter(card -> effectiveSuit(card, levelRank, trumpSuit) == leadPattern.leadSuit())
                .count();
        if (playableLeadSuitCount == 0) {
            return true;
        }
        long followedCount = playedCards.stream()
                .filter(card -> effectiveSuit(card, levelRank, trumpSuit) == leadPattern.leadSuit())
                .count();
        if (followedCount != Math.min(playedCards.size(), playableLeadSuitCount)) {
            return false;
        }
        if (leadPattern.type() == PlayType.PAIR && hasPairOfEffectiveSuit(handBeforePlay, leadPattern.leadSuit(), levelRank, trumpSuit)) {
            return hasPairOfEffectiveSuit(playedCards, leadPattern.leadSuit(), levelRank, trumpSuit);
        }
        if (leadPattern.type() == PlayType.TRACTOR
                && hasTractorOfEffectiveSuit(handBeforePlay, leadPattern.leadSuit(), leadPattern.cardCount() / 2, levelRank, trumpSuit)) {
            return hasTractorOfEffectiveSuit(playedCards, leadPattern.leadSuit(), leadPattern.cardCount() / 2, levelRank, trumpSuit);
        }
        if (leadPattern.type() == PlayType.TRACTOR
                && hasPairOfEffectiveSuit(handBeforePlay, leadPattern.leadSuit(), levelRank, trumpSuit)) {
            int requiredPairs = Math.min(leadPattern.cardCount() / 2,
                    pairCountOfEffectiveSuit(handBeforePlay, leadPattern.leadSuit(), levelRank, trumpSuit));
            return pairCountOfEffectiveSuit(playedCards, leadPattern.leadSuit(), levelRank, trumpSuit) >= requiredPairs;
        }
        return true;
    }

    public static boolean beats(PlayPattern challenger, PlayPattern currentWinner) {
        if (challenger == null) {
            throw new IllegalArgumentException("挑战牌型不能为空");
        }
        if (currentWinner == null) {
            throw new IllegalArgumentException("当前赢家牌型不能为空");
        }
        if (challenger.type() != currentWinner.type() || challenger.cardCount() != currentWinner.cardCount()) {
            return false;
        }
        if (challenger.trump() != currentWinner.trump()) {
            return challenger.trump();
        }
        if (challenger.leadSuit() != currentWinner.leadSuit()) {
            return false;
        }
        return challenger.highWeight() > currentWinner.highWeight();
    }

    public static Suit effectiveSuit(Card card, Rank levelRank, Suit trumpSuit) {
        if (card == null) {
            throw new IllegalArgumentException("牌不能为空");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        if (isTrump(card, levelRank, trumpSuit)) {
            return trumpSuit;
        }
        return card.suit();
    }

    public static Card lowestSingle(List<Card> cards, Rank levelRank, Suit trumpSuit) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("至少选择 1 张牌");
        }
        if (cards.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("牌不能为空");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        return cards.stream()
                .min(Comparator.comparingInt(card -> rankWeight(card, levelRank, trumpSuit)))
                .orElseThrow(() -> new IllegalArgumentException("至少选择 1 张牌"));
    }

    public static boolean isTrump(Card card, Rank levelRank, Suit trumpSuit) {
        if (card == null) {
            throw new IllegalArgumentException("牌不能为空");
        }
        if (levelRank == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        return card.isJoker() || card.rank() == levelRank || card.rank() == Rank.TWO || card.suit() == trumpSuit;
    }

    private static PlayPattern analyzeTractor(List<Card> cards, Rank levelRank, Suit trumpSuit) {
        Map<CardFace, List<Card>> groups = new java.util.HashMap<>();
        for (Card card : cards) {
            CardFace face = new CardFace(card.suit(), card.rank());
            groups.computeIfAbsent(face, ignored -> new ArrayList<>()).add(card);
        }
        if (groups.values().stream().anyMatch(group -> group.size() != 2)) {
            throw new IllegalArgumentException("拖拉机必须由连续对子组成");
        }
        List<Card> pairFaces = groups.values().stream().map(group -> group.get(0))
                .sorted(Comparator.comparingInt(card -> rankWeight(card, levelRank, trumpSuit)))
                .toList();
        Set<Suit> suits = new HashSet<>();
        Set<Boolean> trumpFlags = new HashSet<>();
        for (Card card : pairFaces) {
            suits.add(effectiveSuit(card, levelRank, trumpSuit));
            trumpFlags.add(isTrump(card, levelRank, trumpSuit));
        }
        if (suits.size() != 1 || trumpFlags.size() != 1) {
            throw new IllegalArgumentException("拖拉机必须同花色或同为主牌");
        }
        for (int i = 1; i < pairFaces.size(); i++) {
            int previous = tractorSequenceWeight(pairFaces.get(i - 1), levelRank, trumpSuit);
            int current = tractorSequenceWeight(pairFaces.get(i), levelRank, trumpSuit);
            if (current != previous + 1) {
                throw new IllegalArgumentException("拖拉机对子必须连续");
            }
        }
        Card high = pairFaces.get(pairFaces.size() - 1);
        return new PlayPattern(PlayType.TRACTOR, cards.size(), effectiveSuit(high, levelRank, trumpSuit),
                isTrump(high, levelRank, trumpSuit), rankWeight(high, levelRank, trumpSuit));
    }

    private static boolean sameCardFace(Card left, Card right) {
        return left.suit() == right.suit() && left.rank() == right.rank();
    }

    private static boolean hasPairOfEffectiveSuit(List<Card> cards, Suit suit, Rank levelRank, Suit trumpSuit) {
        return pairCountOfEffectiveSuit(cards, suit, levelRank, trumpSuit) > 0;
    }

    private static int pairCountOfEffectiveSuit(List<Card> cards, Suit suit, Rank levelRank, Suit trumpSuit) {
        Map<CardFace, Long> counts = cards.stream()
                .filter(card -> effectiveSuit(card, levelRank, trumpSuit) == suit)
                .collect(java.util.stream.Collectors.groupingBy(card -> new CardFace(card.suit(), card.rank()),
                        java.util.stream.Collectors.counting()));
        return counts.values().stream()
                .mapToInt(count -> (int) (count / 2))
                .sum();
    }

    private static boolean hasTractorOfEffectiveSuit(List<Card> cards, Suit suit, int requiredPairs, Rank levelRank, Suit trumpSuit) {
        Map<CardFace, Long> counts = cards.stream()
                .filter(card -> effectiveSuit(card, levelRank, trumpSuit) == suit)
                .collect(java.util.stream.Collectors.groupingBy(card -> new CardFace(card.suit(), card.rank()),
                        java.util.stream.Collectors.counting()));
        List<Card> pairFaces = counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(entry -> new Card(entry.getKey().suit(), entry.getKey().rank(), 0))
                .sorted(Comparator.comparingInt(card -> tractorSequenceWeight(card, levelRank, trumpSuit)))
                .toList();
        int consecutivePairs = 0;
        Integer previousWeight = null;
        for (Card pairFace : pairFaces) {
            int currentWeight = tractorSequenceWeight(pairFace, levelRank, trumpSuit);
            if (previousWeight != null && currentWeight == previousWeight + 1) {
                consecutivePairs++;
            } else {
                consecutivePairs = 1;
            }
            if (consecutivePairs >= requiredPairs) {
                return true;
            }
            previousWeight = currentWeight;
        }
        return false;
    }

    private static int rankWeight(Card card, Rank levelRank, Suit trumpSuit) {
        if (card.rank() == Rank.BIG_JOKER) {
            return 1000;
        }
        if (card.rank() == Rank.SMALL_JOKER) {
            return 990;
        }
        if (card.rank() == levelRank && card.suit() == trumpSuit) {
            return 980;
        }
        if (card.rank() == levelRank) {
            return 970 + suitWeight(card.suit());
        }
        if (card.rank() == Rank.TWO && card.suit() == trumpSuit) {
            return 960;
        }
        if (card.rank() == Rank.TWO) {
            return 950 + suitWeight(card.suit());
        }
        return naturalRankWeight(card.rank());
    }

    private static int tractorSequenceWeight(Card card, Rank levelRank, Suit trumpSuit) {
        if (card.isJoker() || card.rank() == levelRank || card.rank() == Rank.TWO) {
            return rankWeight(card, levelRank, trumpSuit);
        }
        return naturalRankWeight(card.rank());
    }

    private static int naturalRankWeight(Rank rank) {
        return switch (rank) {
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
            case SIX -> 6;
            case SEVEN -> 7;
            case EIGHT -> 8;
            case NINE -> 9;
            case TEN -> 10;
            case JACK -> 11;
            case QUEEN -> 12;
            case KING -> 13;
            case ACE -> 14;
            case TWO -> 15;
            case SMALL_JOKER -> 16;
            case BIG_JOKER -> 17;
        };
    }

    private static int suitWeight(Suit suit) {
        return switch (suit) {
            case SPADE -> 0;
            case HEART -> 1;
            case CLUB -> 2;
            case DIAMOND -> 3;
        };
    }

    private record CardFace(Suit suit, Rank rank) {
    }
}
