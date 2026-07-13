package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.rules.ScoreRules;
import com.chesscard.shengji.rules.TrickRules;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AiPlayStrategy {
    public List<Card> choosePlay(GameState game, PlayerSeat seat) {
        List<Card> hand = game.getHands().get(seat);
        if (hand.isEmpty()) {
            return List.of();
        }
        if (game.getCurrentTrick().isEmpty() || game.getCurrentTrickLeader() == null) {
            return List.of(hand.get(0));
        }

        List<Card> leadCards = game.getCurrentTrick().get(game.getCurrentTrickLeader());
        TrickRules.PlayPattern leadPattern = analyzeOrNull(leadCards, game);
        if (leadPattern == null) {
            return chooseForThrowLead(hand, leadCards, game);
        }

        List<Card> matchingSuit = hand.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == leadPattern.leadSuit())
                .toList();
        if (matchingSuit.isEmpty() && !leadPattern.trump()) {
            List<Card> scoredChoice = chooseVoidSuitScoreAwarePlay(hand, leadPattern, game, seat);
            if (!scoredChoice.isEmpty()) {
                return scoredChoice;
            }
        }

        if (leadPattern.type() == TrickRules.PlayType.TRACTOR) {
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
            List<Card> pair = findPair(matchingSuit);
            if (!pair.isEmpty()) {
                return pair;
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

    private List<Card> chooseVoidSuitScoreAwarePlay(
            List<Card> hand,
            TrickRules.PlayPattern leadPattern,
            GameState game,
            PlayerSeat seat) {
        if (currentTrickPoints(game) == 0) {
            return lowestValueCards(hand, leadPattern.cardCount(), game);
        }
        if (!shouldTryToWinCurrentTrick(game, seat)) {
            return lowestValueCards(hand, leadPattern.cardCount(), game);
        }
        List<Card> winningTrump = lowestWinningTrump(hand, leadPattern, game);
        if (!winningTrump.isEmpty()) {
            return winningTrump;
        }
        return lowestValueCards(hand, leadPattern.cardCount(), game);
    }

    private int currentTrickPoints(GameState game) {
        return game.getCurrentTrick().values().stream()
                .flatMap(List::stream)
                .mapToInt(ScoreRules::cardPoints)
                .sum();
    }

    private boolean shouldTryToWinCurrentTrick(GameState game, PlayerSeat seat) {
        PlayerSeat currentWinner = currentWinner(game);
        return isAttacker(seat) != isAttacker(currentWinner);
    }

    private PlayerSeat currentWinner(GameState game) {
        PlayerSeat winner = game.getCurrentTrickLeader();
        TrickRules.PlayPattern winningPattern = analyzeOrNull(game.getCurrentTrick().get(winner), game);
        for (PlayerSeat candidate = winner.next(); candidate != winner; candidate = candidate.next()) {
            List<Card> candidateCards = game.getCurrentTrick().get(candidate);
            if (candidateCards == null) {
                continue;
            }
            TrickRules.PlayPattern challenger = analyzeOrNull(candidateCards, game);
            if (challenger != null && winningPattern != null && TrickRules.beats(challenger, winningPattern)) {
                winner = candidate;
                winningPattern = challenger;
            }
        }
        return winner;
    }

    private boolean isAttacker(PlayerSeat seat) {
        return seat == PlayerSeat.WEST || seat == PlayerSeat.EAST;
    }

    private List<Card> lowestWinningTrump(List<Card> hand, TrickRules.PlayPattern leadPattern, GameState game) {
        List<Card> trumpCards = trumpCards(hand, game);
        List<List<Card>> candidates = switch (leadPattern.type()) {
            case SINGLE -> trumpCards.stream().map(List::of).toList();
            case PAIR -> candidatePairs(trumpCards);
            case TRACTOR -> candidateTractors(trumpCards, leadPattern.cardCount(), game);
        };
        TrickRules.PlayPattern currentWinnerPattern = analyzeOrNull(game.getCurrentTrick().get(currentWinner(game)), game);
        return candidates.stream()
                .filter(candidate -> beatsCurrentWinner(candidate, currentWinnerPattern, game))
                .min(Comparator.comparingInt(candidate -> playCost(candidate, game)))
                .orElse(List.of());
    }

    private boolean beatsCurrentWinner(List<Card> candidate, TrickRules.PlayPattern currentWinnerPattern, GameState game) {
        TrickRules.PlayPattern candidatePattern = analyzeOrNull(candidate, game);
        return candidatePattern != null && currentWinnerPattern != null && TrickRules.beats(candidatePattern, currentWinnerPattern);
    }

    private List<List<Card>> candidatePairs(List<Card> cards) {
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
        return pairs;
    }

    private List<List<Card>> candidateTractors(List<Card> cards, int cardCount, GameState game) {
        List<List<Card>> tractors = new ArrayList<>();
        List<List<Card>> pairs = candidatePairs(cards).stream()
                .sorted(Comparator.comparingInt(pair -> cardWeight(pair.get(0), game)))
                .toList();
        int requiredPairs = cardCount / 2;
        for (int start = 0; start <= pairs.size() - requiredPairs; start++) {
            List<Card> candidate = new ArrayList<>();
            for (int offset = 0; offset < requiredPairs; offset++) {
                candidate.addAll(pairs.get(start + offset));
            }
            TrickRules.PlayPattern pattern = analyzeOrNull(candidate, game);
            if (pattern != null && pattern.type() == TrickRules.PlayType.TRACTOR && pattern.cardCount() == cardCount) {
                tractors.add(candidate);
            }
        }
        return tractors;
    }

    private List<Card> lowestValueCards(List<Card> hand, int cardCount, GameState game) {
        return hand.stream()
                .sorted(Comparator.comparingInt(card -> cardCost(card, game)))
                .limit(cardCount)
                .toList();
    }

    private int playCost(List<Card> cards, GameState game) {
        return cards.stream().mapToInt(card -> cardCost(card, game)).sum();
    }

    private int cardCost(Card card, GameState game) {
        return ScoreRules.cardPoints(card) * 10_000
                + (TrickRules.isTrump(card, game.getLevelRank(), game.getTrumpSuit()) ? 1_000 : 0)
                + cardWeight(card, game);
    }

    private int cardWeight(Card card, GameState game) {
        return TrickRules.analyze(List.of(card), game.getLevelRank(), game.getTrumpSuit()).highWeight();
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
        return candidateTractors(cards, cardCount, game).stream()
                .min(Comparator.comparingInt(candidate -> playCost(candidate, game)))
                .orElse(List.of());
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
