package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeckFactoryTest {
    @Test
    void createsTwoDecksWith108UniqueCards() {
        List<Card> cards = DeckFactory.createDoubleDeck();

        assertThat(cards).hasSize(108);
        assertThat(Set.copyOf(cards)).hasSize(108);
    }
}
