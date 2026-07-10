package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShengjiSorterTest {
    @Test
    void rejectsMissingHandWhenSorting() {
        assertThatThrownBy(() -> ShengjiSorter.sortHand(null, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCardWhenSorting() {
        assertThatThrownBy(() -> ShengjiSorter.sortHand(Collections.singletonList(null), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenSorting() {
        List<Card> hand = List.of(new Card(Suit.HEART, Rank.ACE, 0));

        assertThatThrownBy(() -> ShengjiSorter.sortHand(hand, null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenSorting() {
        List<Card> hand = List.of(new Card(Suit.HEART, Rank.ACE, 0));

        assertThatThrownBy(() -> ShengjiSorter.sortHand(hand, Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sortsByTrumpRulesForHeartAceGame() {
        Card big = new Card(null, Rank.BIG_JOKER, 0);
        Card small = new Card(null, Rank.SMALL_JOKER, 0);
        Card heartAce = new Card(Suit.HEART, Rank.ACE, 0);
        Card spadeAce = new Card(Suit.SPADE, Rank.ACE, 0);
        Card heartTwo = new Card(Suit.HEART, Rank.TWO, 0);
        Card clubTwo = new Card(Suit.CLUB, Rank.TWO, 0);
        Card heartKing = new Card(Suit.HEART, Rank.KING, 0);
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);

        List<Card> sorted = ShengjiSorter.sortHand(
                List.of(spadeKing, clubTwo, heartKing, small, heartTwo, spadeAce, big, heartAce),
                Rank.ACE,
                Suit.HEART
        );

        assertThat(sorted).containsExactly(big, small, heartAce, spadeAce, heartTwo, clubTwo, heartKing, spadeKing);
    }

    @Test
    void sortsByTrumpRulesForSpadeFiveGame() {
        Card big = new Card(null, Rank.BIG_JOKER, 0);
        Card small = new Card(null, Rank.SMALL_JOKER, 0);
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        Card heartFive = new Card(Suit.HEART, Rank.FIVE, 0);
        Card spadeTwo = new Card(Suit.SPADE, Rank.TWO, 0);
        Card heartTwo = new Card(Suit.HEART, Rank.TWO, 0);
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);
        Card heartKing = new Card(Suit.HEART, Rank.KING, 0);

        List<Card> sorted = ShengjiSorter.sortHand(
                List.of(heartKing, spadeTwo, heartFive, big, spadeKing, small, heartTwo, spadeFive),
                Rank.FIVE,
                Suit.SPADE
        );

        assertThat(sorted).containsExactly(big, small, spadeFive, heartFive, spadeTwo, heartTwo, spadeKing, heartKing);
    }

    @Test
    void sortsNonTrumpCardsBySuitThenRank() {
        Card heartAce = new Card(Suit.HEART, Rank.ACE, 0);
        Card heartKing = new Card(Suit.HEART, Rank.KING, 0);
        Card spadeAce = new Card(Suit.SPADE, Rank.ACE, 0);
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);
        Card clubAce = new Card(Suit.CLUB, Rank.ACE, 0);
        Card diamondAce = new Card(Suit.DIAMOND, Rank.ACE, 0);

        List<Card> sorted = ShengjiSorter.sortHand(
                List.of(diamondAce, clubAce, heartKing, spadeKing, heartAce, spadeAce),
                Rank.THREE,
                Suit.HEART
        );

        assertThat(sorted).containsExactly(heartAce, heartKing, spadeAce, spadeKing, clubAce, diamondAce);
    }
}
