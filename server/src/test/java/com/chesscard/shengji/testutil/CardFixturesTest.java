package com.chesscard.shengji.testutil;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardFixturesTest {
    @Test
    void createsSingleCard() {
        Card card = CardFixtures.card(Suit.CLUB, Rank.FIVE, 1);

        assertThat(card.suit()).isEqualTo(Suit.CLUB);
        assertThat(card.rank()).isEqualTo(Rank.FIVE);
        assertThat(card.deckIndex()).isEqualTo(1);
    }

    @Test
    void createsPhysicalPair() {
        List<Card> pair = CardFixtures.pair(Suit.SPADE, Rank.KING);

        assertThat(pair).containsExactly(
                new Card(Suit.SPADE, Rank.KING, 0),
                new Card(Suit.SPADE, Rank.KING, 1)
        );
    }

    @Test
    void createsConsecutivePairsAsTractor() {
        List<Card> tractor = CardFixtures.tractor(Suit.HEART, Rank.FIVE, Rank.SIX);

        assertThat(tractor).containsExactly(
                new Card(Suit.HEART, Rank.FIVE, 0),
                new Card(Suit.HEART, Rank.FIVE, 1),
                new Card(Suit.HEART, Rank.SIX, 0),
                new Card(Suit.HEART, Rank.SIX, 1)
        );
    }
}