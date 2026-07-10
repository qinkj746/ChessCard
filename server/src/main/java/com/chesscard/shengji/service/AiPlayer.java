package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.rules.TrickRules;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class AiPlayer {
    public List<Card> choosePlay(GameState game, PlayerSeat seat) {
        if (game == null) {
            throw new IllegalArgumentException("牌局状态不能为空");
        }
        if (game.getPhase() == null) {
            throw new IllegalArgumentException("牌局阶段不能为空");
        }
        if (game.getPhase() != GamePhase.PLAY) {
            throw new IllegalArgumentException("当前阶段不能出牌");
        }
        if (seat == null) {
            throw new IllegalArgumentException("玩家座位不能为空");
        }
        if (seat == PlayerSeat.SOUTH) {
            throw new IllegalArgumentException("真人玩家不能由 AI 出牌");
        }
        if (game.getLevelRank() == null) {
            throw new IllegalArgumentException("级牌不能为空");
        }
        if (game.getTrumpSuit() == null) {
            throw new IllegalArgumentException("主花色不能为空");
        }
        if (!game.getHands().containsKey(seat)) {
            throw new IllegalArgumentException("玩家手牌不能为空");
        }
        List<Card> hand = game.getHands().get(seat);
        if (hand == null) {
            throw new IllegalArgumentException("玩家手牌不能为空");
        }
        validateHandCards(hand);
        if (hand.stream().distinct().count() != hand.size()) {
            throw new IllegalArgumentException("玩家手牌不能包含重复牌");
        }
        if (game.getCurrentTrick().containsKey(seat)) {
            throw new IllegalArgumentException("玩家已在当前墩出牌");
        }
        List<Card> currentTrickCards = validateCurrentTrickCards(game.getCurrentTrick().values());
        long currentTrickCardCount = currentTrickCards.size();
        long uniqueCurrentTrickCardCount = currentTrickCards.stream()
                .distinct()
                .count();
        if (uniqueCurrentTrickCardCount != currentTrickCardCount) {
            throw new IllegalArgumentException("当前墩出牌不能包含重复牌");
        }
        if (hand.stream().anyMatch(card -> currentTrickCards.stream().anyMatch(card::equals))) {
            throw new IllegalArgumentException("玩家手牌不能包含当前墩已出的牌");
        }
        if (game.getCurrentTrickLeader() != null && !game.getCurrentTrick().containsKey(game.getCurrentTrickLeader())) {
            throw new IllegalArgumentException("首攻出牌不能为空");
        }
        if (game.getCurrentTrickLeader() == null && !game.getCurrentTrick().isEmpty()) {
            throw new IllegalArgumentException("首攻座位不能为空");
        }
        if (game.getCurrentTrickLeader() != null && hasLaterSeatEntry(game, seat)) {
            throw new IllegalArgumentException("当前墩出牌顺序不合法");
        }
        if (game.getCurrentTrickLeader() != null && hasMissingEarlierSeatWithCards(game, seat)) {
            throw new IllegalArgumentException("当前墩出牌顺序不合法");
        }
        if (game.getCurrentTurn() == null) {
            throw new IllegalArgumentException("当前行动座位不能为空");
        }
        if (game.getCurrentTurn() != seat) {
            throw new IllegalArgumentException("还没有轮到该玩家");
        }
        if (hand.isEmpty()) {
            return List.of();
        }
        if (game.getCurrentTrick().isEmpty() || game.getCurrentTrickLeader() == null) {
            return List.of(hand.get(0));
        }
        List<Card> leadCards = game.getCurrentTrick().get(game.getCurrentTrickLeader());
        if (leadCards == null) {
            throw new IllegalArgumentException("首攻出牌不能为空");
        }
        if (leadCards.isEmpty()) {
            throw new IllegalArgumentException("首攻出牌不能为空");
        }
        if (leadCards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("首攻出牌不能包含空牌");
        }
        if (game.getCurrentTrick().entrySet().stream()
                .filter(entry -> entry.getKey() != game.getCurrentTrickLeader())
                .anyMatch(entry -> entry.getValue().size() != leadCards.size())) {
            throw new IllegalArgumentException("当前墩出牌张数不一致");
        }
        if (hand.size() < leadCards.size()) {
            throw new IllegalArgumentException("玩家手牌数量不足以跟牌");
        }
        TrickRules.PlayPattern leadPattern = analyzeOrNull(leadCards, game);
        if (leadPattern == null) {
            return chooseForThrowLead(hand, leadCards, game);
        }
        List<Card> matchingSuit = hand.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == leadPattern.leadSuit())
                .toList();
        if (leadPattern.type() == TrickRules.PlayType.TRACTOR) {
            if (matchingSuit.isEmpty() && !leadPattern.trump()) {
                List<Card> trumpCards = trumpCards(hand, game);
                List<Card> trumpTractor = findTractor(trumpCards, leadPattern.cardCount(), game);
                if (!trumpTractor.isEmpty()) {
                    return trumpTractor;
                }
            }
            List<Card> tractor = findTractor(matchingSuit, leadPattern.cardCount(), game);
            if (!tractor.isEmpty()) {
                return tractor;
            }
            List<Card> pairFallback = choosePairsThenFill(hand, matchingSuit, leadPattern.cardCount());
            if (!pairFallback.isEmpty()) {
                return pairFallback;
            }
        }
        if (leadPattern.type() == TrickRules.PlayType.PAIR) {
            if (matchingSuit.isEmpty() && !leadPattern.trump()) {
                List<Card> trumpCards = trumpCards(hand, game);
                List<Card> trumpPair = findPair(trumpCards);
                if (!trumpPair.isEmpty()) {
                    return trumpPair;
                }
            }
            List<Card> pair = findPair(matchingSuit);
            if (!pair.isEmpty()) {
                return pair;
            }
        }
        if (leadPattern.type() == TrickRules.PlayType.SINGLE && matchingSuit.isEmpty() && !leadPattern.trump()) {
            List<Card> trumpCards = trumpCards(hand, game);
            if (!trumpCards.isEmpty()) {
                return List.of(trumpCards.get(0));
            }
        }
        List<Card> selected = new ArrayList<>(matchingSuit.subList(0, Math.min(leadPattern.cardCount(), matchingSuit.size())));
        if (selected.size() < leadPattern.cardCount()) {
            for (Card card : hand) {
                if (!selected.contains(card)) {
                    selected.add(card);
                }
                if (selected.size() == leadPattern.cardCount()) {
                    break;
                }
            }
        }
        return selected;
    }

    private boolean hasLaterSeatEntry(GameState game, PlayerSeat seat) {
        for (PlayerSeat nextSeat = seat.next(); nextSeat != game.getCurrentTrickLeader(); nextSeat = nextSeat.next()) {
            if (game.getCurrentTrick().containsKey(nextSeat)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMissingEarlierSeatWithCards(GameState game, PlayerSeat seat) {
        for (PlayerSeat previousSeat = game.getCurrentTrickLeader().next(); previousSeat != seat; previousSeat = previousSeat.next()) {
            List<Card> previousHand = game.getHands().get(previousSeat);
            if (!game.getCurrentTrick().containsKey(previousSeat) && (previousHand == null || !previousHand.isEmpty())) {
                return true;
            }
        }
        return false;
    }

    private void validateHandCards(List<Card> hand) {
        validateCardFaces(
                hand,
                "玩家手牌不能包含空牌",
                "玩家手牌不能缺少点数",
                "玩家手牌大小王不能包含花色",
                "玩家手牌普通牌不能缺少花色",
                "玩家手牌牌副索引必须为 0 或 1");
    }

    private List<Card> validateCurrentTrickCards(Collection<List<Card>> trickPlays) {
        if (trickPlays.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("当前墩出牌不能为空");
        }
        if (trickPlays.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("当前墩出牌不能为空");
        }
        List<Card> cards = trickPlays.stream()
                .flatMap(List::stream)
                .toList();
        validateCardFaces(
                cards,
                "当前墩出牌不能包含空牌",
                "当前墩出牌不能缺少点数",
                "当前墩出牌大小王不能包含花色",
                "当前墩出牌普通牌不能缺少花色",
                "当前墩出牌牌副索引必须为 0 或 1");
        return cards;
    }

    private void validateCardFaces(
            List<Card> cards,
            String nullCardMessage,
            String missingRankMessage,
            String jokerSuitMessage,
            String nonJokerMissingSuitMessage,
            String invalidDeckIndexMessage) {
        if (cards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(nullCardMessage);
        }
        if (cards.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException(missingRankMessage);
        }
        if (cards.stream().anyMatch(this::hasSuitForJoker)) {
            throw new IllegalArgumentException(jokerSuitMessage);
        }
        if (cards.stream().anyMatch(this::isMissingSuitForNonJoker)) {
            throw new IllegalArgumentException(nonJokerMissingSuitMessage);
        }
        if (cards.stream().anyMatch(this::hasInvalidDeckIndex)) {
            throw new IllegalArgumentException(invalidDeckIndexMessage);
        }
    }

    private boolean isMissingSuitForNonJoker(Card card) {
        return card.suit() == null && card.rank() != Rank.SMALL_JOKER && card.rank() != Rank.BIG_JOKER;
    }

    private boolean hasSuitForJoker(Card card) {
        return card.suit() != null && (card.rank() == Rank.SMALL_JOKER || card.rank() == Rank.BIG_JOKER);
    }

    private boolean hasInvalidDeckIndex(Card card) {
        return card.deckIndex() < 0 || card.deckIndex() > 1;
    }

    private List<Card> choosePairsThenFill(List<Card> hand, List<Card> matchingSuit, int cardCount) {
        List<Card> selected = new ArrayList<>();
        List<Card> remaining = new ArrayList<>(matchingSuit);
        while (selected.size() + 1 < cardCount) {
            List<Card> pair = findPair(remaining);
            if (pair.isEmpty()) {
                break;
            }
            selected.addAll(pair);
            remaining.removeAll(pair);
        }
        if (selected.isEmpty()) {
            return List.of();
        }
        for (Card card : hand) {
            if (!selected.contains(card)) {
                selected.add(card);
            }
            if (selected.size() == cardCount) {
                break;
            }
        }
        return selected;
    }

    private List<Card> findPair(List<Card> cards) {
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                Card left = cards.get(i);
                Card right = cards.get(j);
                if (left.suit() == right.suit() && left.rank() == right.rank()) {
                    return List.of(left, right);
                }
            }
        }
        return List.of();
    }

    private List<Card> findTractor(List<Card> cards, int cardCount, GameState game) {
        List<List<Card>> pairs = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                Card left = cards.get(i);
                Card right = cards.get(j);
                if (left.suit() == right.suit() && left.rank() == right.rank()) {
                    pairs.add(List.of(left, right));
                }
            }
        }
        pairs.sort(java.util.Comparator.comparingInt(pair -> pair.get(0).rank().ordinal()));
        int requiredPairs = cardCount / 2;
        for (int start = 0; start <= pairs.size() - requiredPairs; start++) {
            List<Card> candidate = new ArrayList<>();
            for (int offset = 0; offset < requiredPairs; offset++) {
                candidate.addAll(pairs.get(start + offset));
            }
            TrickRules.PlayPattern pattern = analyzeOrNull(candidate, game);
            if (pattern != null && pattern.type() == TrickRules.PlayType.TRACTOR && pattern.cardCount() == cardCount) {
                return candidate;
            }
        }
        return List.of();
    }

    private List<Card> chooseForThrowLead(List<Card> hand, List<Card> leadCards, GameState game) {
        Suit leadSuit = TrickRules.effectiveSuit(leadCards.get(0), game.getLevelRank(), game.getTrumpSuit());
        List<Card> matchingSuit = hand.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == leadSuit)
                .toList();
        if (matchingSuit.isEmpty() && leadSuit != game.getTrumpSuit()) {
            List<Card> trumpCards = trumpCards(hand, game);
            if (trumpCards.size() >= leadCards.size()) {
                return new ArrayList<>(trumpCards.subList(0, leadCards.size()));
            }
        }
        List<Card> selected = new ArrayList<>(matchingSuit.subList(0, Math.min(leadCards.size(), matchingSuit.size())));
        if (selected.size() < leadCards.size()) {
            for (Card card : hand) {
                if (!selected.contains(card)) {
                    selected.add(card);
                }
                if (selected.size() == leadCards.size()) {
                    break;
                }
            }
        }
        return selected;
    }

    private List<Card> trumpCards(List<Card> cards, GameState game) {
        return cards.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == game.getTrumpSuit())
                .toList();
    }

    private TrickRules.PlayPattern analyzeOrNull(List<Card> cards, GameState game) {
        try {
            return TrickRules.analyze(cards, game.getLevelRank(), game.getTrumpSuit());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
