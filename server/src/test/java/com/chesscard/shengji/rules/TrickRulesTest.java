package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.chesscard.shengji.testutil.CardFixtures.card;
import static com.chesscard.shengji.testutil.CardFixtures.pair;
import static com.chesscard.shengji.testutil.CardFixtures.tractor;

class TrickRulesTest {
    @Test
    void requiresFollowingLeadSuitWhenPossible() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = new Card(Suit.SPADE, Rank.SIX, 0);
        Card heartKing = new Card(Suit.HEART, Rank.KING, 0);

        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThat(TrickRules.followsLead(List.of(heartKing), List.of(spadeSix, heartKing), lead, Rank.ACE, Suit.HEART))
                .isFalse();
        assertThat(TrickRules.followsLead(List.of(spadeSix), List.of(spadeSix, heartKing), lead, Rank.ACE, Suit.HEART))
                .isTrue();
    }

    @Test
    void trumpSingleBeatsNonTrumpSingle() {
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);
        Card heartThree = new Card(Suit.HEART, Rank.THREE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeKing), Rank.ACE, Suit.HEART);
        TrickRules.PlayPattern challenger = TrickRules.analyze(List.of(heartThree), Rank.ACE, Suit.HEART);

        assertThat(TrickRules.beats(challenger, lead)).isTrue();
    }

    @Test
    void rejectsMissingChallengerPatternWhenComparingTricks() {
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);
        TrickRules.PlayPattern currentWinner = TrickRules.analyze(List.of(spadeKing), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.beats(null, currentWinner))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCurrentWinnerPatternWhenComparingTricks() {
        Card heartThree = new Card(Suit.HEART, Rank.THREE, 0);
        TrickRules.PlayPattern challenger = TrickRules.analyze(List.of(heartThree), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.beats(challenger, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardListWhenFindingLowestSingle() {
        assertThatThrownBy(() -> TrickRules.lowestSingle(null, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCardWhenFindingLowestSingle() {
        assertThatThrownBy(() -> TrickRules.lowestSingle(Collections.singletonList(null), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenFindingLowestSingle() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.lowestSingle(List.of(clubFive), null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenFindingLowestSingle() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.lowestSingle(List.of(clubFive), Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardWhenResolvingEffectiveSuit() {
        assertThatThrownBy(() -> TrickRules.effectiveSuit(null, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenResolvingEffectiveSuit() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.effectiveSuit(clubFive, null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenResolvingEffectiveSuit() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.effectiveSuit(clubFive, Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardWhenCheckingTrump() {
        assertThatThrownBy(() -> TrickRules.isTrump(null, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenCheckingTrump() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.isTrump(clubFive, null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenCheckingTrump() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.isTrump(clubFive, Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recognizesBasicTractor() {
        List<Card> cards = tractor(Suit.CLUB, Rank.FIVE, Rank.SIX);

        TrickRules.PlayPattern pattern = TrickRules.analyze(cards, Rank.ACE, Suit.HEART);

        assertThat(pattern.type()).isEqualTo(TrickRules.PlayType.TRACTOR);
        assertThat(pattern.cardCount()).isEqualTo(4);
    }

    @Test
    void rejectsDuplicatePhysicalCardInPattern() {
        Card clubFive = card(Suit.CLUB, Rank.FIVE, 0);
        List<Card> clubSixPair = pair(Suit.CLUB, Rank.SIX);

        assertThatThrownBy(() -> TrickRules.analyze(List.of(clubFive, clubFive), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrickRules.analyze(
                List.of(clubFive, clubFive, clubSixPair.get(0), clubSixPair.get(1)), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardListInPattern() {
        assertThatThrownBy(() -> TrickRules.analyze(null, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCardInPattern() {
        assertThatThrownBy(() -> TrickRules.analyze(Collections.singletonList(null), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankInPattern() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.analyze(List.of(clubFive), null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitInPattern() {
        Card clubFive = new Card(Suit.CLUB, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.analyze(List.of(clubFive), Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingPlayedCardsWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(null, List.of(spadeFive), lead, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingHandWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(List.of(spadeFive), null, lead, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLeadPatternWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);

        assertThatThrownBy(() -> TrickRules.followsLead(List.of(spadeFive), List.of(spadeFive), null,
                Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPlayedCardWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(Collections.singletonList(null), List.of(spadeFive),
                lead, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullHandCardWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(List.of(spadeFive), Collections.singletonList(null),
                lead, Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(List.of(spadeFive), List.of(spadeFive), lead,
                null, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenCheckingFollowSuit() {
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(spadeFive), Rank.ACE, Suit.HEART);

        assertThatThrownBy(() -> TrickRules.followsLead(List.of(spadeFive), List.of(spadeFive), lead,
                Rank.ACE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresFollowingPairWhenPlayerHasLeadSuitPair() {
        List<Card> leadPair = pair(Suit.SPADE, Rank.FIVE);
        List<Card> playablePair = pair(Suit.SPADE, Rank.SIX);
        Card spadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(leadPair, Rank.ACE, Suit.HEART);

        assertThat(TrickRules.followsLead(List.of(playablePair.get(0), spadeSeven),
                List.of(playablePair.get(0), playablePair.get(1), spadeSeven), lead, Rank.ACE, Suit.HEART))
                .isFalse();
        assertThat(TrickRules.followsLead(playablePair,
                List.of(playablePair.get(0), playablePair.get(1), spadeSeven), lead, Rank.ACE, Suit.HEART))
                .isTrue();
    }

    @Test
    void requiresFollowingTractorWhenPlayerHasLeadSuitTractor() {
        List<Card> leadCards = tractor(Suit.CLUB, Rank.FIVE, Rank.SIX);
        List<Card> sevenPair = pair(Suit.CLUB, Rank.SEVEN);
        List<Card> eightPair = pair(Suit.CLUB, Rank.EIGHT);
        List<Card> tenPair = pair(Suit.CLUB, Rank.TEN);
        TrickRules.PlayPattern lead = TrickRules.analyze(leadCards, Rank.ACE, Suit.HEART);

        assertThat(TrickRules.followsLead(List.of(sevenPair.get(0), sevenPair.get(1), tenPair.get(0), tenPair.get(1)),
                List.of(sevenPair.get(0), sevenPair.get(1), eightPair.get(0), eightPair.get(1), tenPair.get(0), tenPair.get(1)),
                lead, Rank.ACE, Suit.HEART))
                .isFalse();
        assertThat(TrickRules.followsLead(List.of(sevenPair.get(0), sevenPair.get(1), eightPair.get(0), eightPair.get(1)),
                List.of(sevenPair.get(0), sevenPair.get(1), eightPair.get(0), eightPair.get(1), tenPair.get(0), tenPair.get(1)),
                lead, Rank.ACE, Suit.HEART))
                .isTrue();
    }

    @Test
    void requiresFollowingPairWhenPlayerHasPairButNoTractorAgainstLeadTractor() {
        Card leadFive0 = new Card(Suit.CLUB, Rank.FIVE, 0);
        Card leadFive1 = new Card(Suit.CLUB, Rank.FIVE, 1);
        Card leadSix0 = new Card(Suit.CLUB, Rank.SIX, 0);
        Card leadSix1 = new Card(Suit.CLUB, Rank.SIX, 1);
        Card eight0 = new Card(Suit.CLUB, Rank.EIGHT, 0);
        Card eight1 = new Card(Suit.CLUB, Rank.EIGHT, 1);
        Card ten0 = new Card(Suit.CLUB, Rank.TEN, 0);
        Card jack0 = new Card(Suit.CLUB, Rank.JACK, 0);
        Card queen0 = new Card(Suit.CLUB, Rank.QUEEN, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(List.of(leadFive0, leadFive1, leadSix0, leadSix1),
                Rank.ACE, Suit.HEART);

        assertThat(TrickRules.followsLead(List.of(eight0, ten0, jack0, queen0),
                List.of(eight0, eight1, ten0, jack0, queen0), lead, Rank.ACE, Suit.HEART))
                .isFalse();
        assertThat(TrickRules.followsLead(List.of(eight0, eight1, ten0, jack0),
                List.of(eight0, eight1, ten0, jack0, queen0), lead, Rank.ACE, Suit.HEART))
                .isTrue();
    }

    @Test
    void requiresFollowingAsManyPairsAsPossibleAgainstLongerLeadTractor() {
        Card leadFive0 = new Card(Suit.CLUB, Rank.FIVE, 0);
        Card leadFive1 = new Card(Suit.CLUB, Rank.FIVE, 1);
        Card leadSix0 = new Card(Suit.CLUB, Rank.SIX, 0);
        Card leadSix1 = new Card(Suit.CLUB, Rank.SIX, 1);
        Card leadSeven0 = new Card(Suit.CLUB, Rank.SEVEN, 0);
        Card leadSeven1 = new Card(Suit.CLUB, Rank.SEVEN, 1);
        Card nine0 = new Card(Suit.CLUB, Rank.NINE, 0);
        Card nine1 = new Card(Suit.CLUB, Rank.NINE, 1);
        Card jack0 = new Card(Suit.CLUB, Rank.JACK, 0);
        Card jack1 = new Card(Suit.CLUB, Rank.JACK, 1);
        Card ten0 = new Card(Suit.CLUB, Rank.TEN, 0);
        Card queen0 = new Card(Suit.CLUB, Rank.QUEEN, 0);
        Card king0 = new Card(Suit.CLUB, Rank.KING, 0);
        TrickRules.PlayPattern lead = TrickRules.analyze(
                List.of(leadFive0, leadFive1, leadSix0, leadSix1, leadSeven0, leadSeven1),
                Rank.ACE, Suit.HEART);

        assertThat(TrickRules.followsLead(List.of(nine0, nine1, jack0, queen0, king0, jack1),
                List.of(nine0, nine1, jack0, jack1, ten0, queen0, king0), lead, Rank.ACE, Suit.HEART))
                .isTrue();
        assertThat(TrickRules.followsLead(List.of(nine0, nine1, jack0, ten0, queen0, king0),
                List.of(nine0, nine1, jack0, jack1, ten0, queen0, king0), lead, Rank.ACE, Suit.HEART))
                .isFalse();
    }

    @Test
    void tractorRequiresSameCardCountToBeat() {
        TrickRules.PlayPattern clubTractor = TrickRules.analyze(
                tractor(Suit.CLUB, Rank.FIVE, Rank.SIX), Rank.ACE, Suit.HEART);
        TrickRules.PlayPattern heartPair = TrickRules.analyze(
                pair(Suit.HEART, Rank.FIVE), Rank.ACE, Suit.HEART);

        assertThat(TrickRules.beats(heartPair, clubTractor)).isFalse();
    }

    @Test
    void trumpTractorCanBeatNonTrumpTractorOfSameLength() {
        Card clubFive0 = new Card(Suit.CLUB, Rank.FIVE, 0);
        Card clubFive1 = new Card(Suit.CLUB, Rank.FIVE, 1);
        Card clubSix0 = new Card(Suit.CLUB, Rank.SIX, 0);
        Card clubSix1 = new Card(Suit.CLUB, Rank.SIX, 1);
        Card heartFive0 = new Card(Suit.HEART, Rank.FIVE, 0);
        Card heartFive1 = new Card(Suit.HEART, Rank.FIVE, 1);
        Card heartSix0 = new Card(Suit.HEART, Rank.SIX, 0);
        Card heartSix1 = new Card(Suit.HEART, Rank.SIX, 1);
        TrickRules.PlayPattern clubTractor = TrickRules.analyze(
                List.of(clubFive0, clubFive1, clubSix0, clubSix1), Rank.ACE, Suit.HEART);
        TrickRules.PlayPattern heartTractor = TrickRules.analyze(
                List.of(heartFive0, heartFive1, heartSix0, heartSix1), Rank.ACE, Suit.HEART);

        assertThat(TrickRules.beats(heartTractor, clubTractor)).isTrue();
    }

    @Test
    void higherTrumpTractorBeatsLowerTrumpTractor() {
        Card heartFive0 = new Card(Suit.HEART, Rank.FIVE, 0);
        Card heartFive1 = new Card(Suit.HEART, Rank.FIVE, 1);
        Card heartSix0 = new Card(Suit.HEART, Rank.SIX, 0);
        Card heartSix1 = new Card(Suit.HEART, Rank.SIX, 1);
        Card heartSeven0 = new Card(Suit.HEART, Rank.SEVEN, 0);
        Card heartSeven1 = new Card(Suit.HEART, Rank.SEVEN, 1);
        Card heartEight0 = new Card(Suit.HEART, Rank.EIGHT, 0);
        Card heartEight1 = new Card(Suit.HEART, Rank.EIGHT, 1);
        TrickRules.PlayPattern lowerTractor = TrickRules.analyze(
                List.of(heartFive0, heartFive1, heartSix0, heartSix1), Rank.ACE, Suit.HEART);
        TrickRules.PlayPattern higherTractor = TrickRules.analyze(
                List.of(heartSeven0, heartSeven1, heartEight0, heartEight1), Rank.ACE, Suit.HEART);

        assertThat(TrickRules.beats(higherTractor, lowerTractor)).isTrue();
        assertThat(TrickRules.beats(lowerTractor, higherTractor)).isFalse();
    }

    @Test
    void trumpCardsCanFormTractorWhenConsecutive() {
        Card heartKing0 = new Card(Suit.HEART, Rank.KING, 0);
        Card heartKing1 = new Card(Suit.HEART, Rank.KING, 1);
        Card heartQueen0 = new Card(Suit.HEART, Rank.QUEEN, 0);
        Card heartQueen1 = new Card(Suit.HEART, Rank.QUEEN, 1);
        TrickRules.PlayPattern pattern = TrickRules.analyze(
                List.of(heartKing0, heartKing1, heartQueen0, heartQueen1), Rank.ACE, Suit.HEART);

        assertThat(pattern.type()).isEqualTo(TrickRules.PlayType.TRACTOR);
        assertThat(pattern.cardCount()).isEqualTo(4);
        assertThat(pattern.trump()).isTrue();
    }

    @Test
    void nonConsecutivePairsDoNotFormTractor() {
        Card clubFive0 = new Card(Suit.CLUB, Rank.FIVE, 0);
        Card clubFive1 = new Card(Suit.CLUB, Rank.FIVE, 1);
        Card clubSeven0 = new Card(Suit.CLUB, Rank.SEVEN, 0);
        Card clubSeven1 = new Card(Suit.CLUB, Rank.SEVEN, 1);

        assertThatThrownBy(() -> TrickRules.analyze(
                List.of(clubFive0, clubFive1, clubSeven0, clubSeven1), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mixedSuitPairsDoNotFormTractor() {
        Card clubFive0 = new Card(Suit.CLUB, Rank.FIVE, 0);
        Card clubFive1 = new Card(Suit.CLUB, Rank.FIVE, 1);
        Card spadeSix0 = new Card(Suit.SPADE, Rank.SIX, 0);
        Card spadeSix1 = new Card(Suit.SPADE, Rank.SIX, 1);

        assertThatThrownBy(() -> TrickRules.analyze(
                List.of(clubFive0, clubFive1, spadeSix0, spadeSix1), Rank.ACE, Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
