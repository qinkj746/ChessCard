package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeclarationRulesTest {
    @Test
    void rejectsMissingHandWhenFindingAvailableSuits() {
        assertThatThrownBy(() -> DeclarationRules.availableSuits(null, Rank.ACE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCardWhenFindingAvailableSuits() {
        assertThatThrownBy(() -> DeclarationRules.availableSuits(Collections.singletonList(null), Rank.ACE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenFindingAvailableSuits() {
        List<Card> hand = List.of(new Card(null, Rank.SMALL_JOKER, 0));

        assertThatThrownBy(() -> DeclarationRules.availableSuits(hand, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotDeclareWithoutJoker() {
        List<Card> hand = List.of(new Card(Suit.HEART, Rank.ACE, 0));

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).isEmpty();
    }

    @Test
    void bigJokerAllowsOnlyRedLevelSuitsPresentInHand() {
        List<Card> hand = List.of(
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.HEART, Rank.ACE, 0),
                new Card(Suit.SPADE, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.HEART);
    }

    @Test
    void smallJokerAllowsOnlyBlackLevelSuitsPresentInHand() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(Suit.SPADE, Rank.ACE, 0),
                new Card(Suit.DIAMOND, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.SPADE);
    }

    @Test
    void bothJokersAllowMatchingBlackAndRedLevelSuits() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.SPADE, Rank.ACE, 0),
                new Card(Suit.DIAMOND, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.SPADE, Suit.DIAMOND);
    }

    @Test
    void bigJokerWithoutRedLevelSuitsReturnsEmpty() {
        List<Card> hand = List.of(
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.SPADE, Rank.ACE, 0),
                new Card(Suit.CLUB, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).isEmpty();
    }

    @Test
    void smallJokerWithoutBlackLevelSuitsReturnsEmpty() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(Suit.HEART, Rank.ACE, 0),
                new Card(Suit.DIAMOND, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).isEmpty();
    }

    @Test
    void bothJokersWithoutAnyLevelSuitsReturnsEmpty() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.HEART, Rank.KING, 0),
                new Card(Suit.SPADE, Rank.KING, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).isEmpty();
    }

    @Test
    void bigJokerWithBothRedLevelSuitsReturnsBoth() {
        List<Card> hand = List.of(
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.HEART, Rank.ACE, 0),
                new Card(Suit.DIAMOND, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.HEART, Suit.DIAMOND);
    }

    @Test
    void smallJokerWithBothBlackLevelSuitsReturnsBoth() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(Suit.SPADE, Rank.ACE, 0),
                new Card(Suit.CLUB, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.SPADE, Suit.CLUB);
    }
}
