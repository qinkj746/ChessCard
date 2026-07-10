package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreRulesTest {
    @Test
    void rejectsMissingCardWhenScoringPoints() {
        assertThatThrownBy(() -> ScoreRules.cardPoints(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreCardsAreWorthExpectedPoints() {
        assertThat(ScoreRules.cardPoints(new Card(Suit.HEART, Rank.FIVE, 0))).isEqualTo(5);
        assertThat(ScoreRules.cardPoints(new Card(Suit.HEART, Rank.TEN, 0))).isEqualTo(10);
        assertThat(ScoreRules.cardPoints(new Card(Suit.HEART, Rank.KING, 0))).isEqualTo(10);
        assertThat(ScoreRules.cardPoints(new Card(Suit.HEART, Rank.ACE, 0))).isZero();
    }

    @Test
    void bankerUpgradeStepsFollowShengjiThresholds() {
        assertThat(ScoreRules.bankerDeltaForDefenderWin(0)).isEqualTo(3);
        assertThat(ScoreRules.bankerDeltaForDefenderWin(35)).isEqualTo(2);
        assertThat(ScoreRules.bankerDeltaForDefenderWin(75)).isEqualTo(1);
    }

    @Test
    void rejectsNegativeAttackerScoreWhenCalculatingDefenderWinDelta() {
        assertThatThrownBy(() -> ScoreRules.bankerDeltaForDefenderWin(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void attackersUpgradeIsCappedAtThree() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(80)).isEqualTo(1);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(120)).isEqualTo(2);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(160)).isEqualTo(3);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(200)).isEqualTo(3);
    }

    @Test
    void rejectsNegativeAttackerScoreWhenCalculatingAttackersWinDelta() {
        assertThatThrownBy(() -> ScoreRules.attackerDeltaForAttackersWin(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kittyMultiplierSupportsMainPatterns() {
        assertThat(ScoreRules.kittyMultiplier("SINGLE", 1)).isEqualTo(2);
        assertThat(ScoreRules.kittyMultiplier("PAIR", 1)).isEqualTo(4);
        assertThat(ScoreRules.kittyMultiplier("TRACTOR", 3)).isEqualTo(6);
    }

    @Test
    void rejectsMissingKittyPatternWhenCalculatingMultiplier() {
        assertThatThrownBy(() -> ScoreRules.kittyMultiplier(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveTractorLengthWhenCalculatingKittyMultiplier() {
        assertThatThrownBy(() -> ScoreRules.kittyMultiplier("TRACTOR", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCurrentLevelRankWhenAdvancing() {
        assertThatThrownBy(() -> ScoreRules.advanceLevelRank(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advancesLevelRankAndCapsAtKing() {
        assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 1)).isEqualTo(Rank.TWO);
        assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 3)).isEqualTo(Rank.FOUR);
        assertThat(ScoreRules.advanceLevelRank(Rank.QUEEN, 3)).isEqualTo(Rank.KING);
    }

    @Test
    void defenderWinDeltaBoundaryAt40() {
        assertThat(ScoreRules.bankerDeltaForDefenderWin(39)).isEqualTo(2);
        assertThat(ScoreRules.bankerDeltaForDefenderWin(40)).isEqualTo(1);
    }

    @Test
    void attackerWinDeltaBoundaryAt80() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(79)).isZero();
        assertThat(ScoreRules.attackerDeltaForAttackersWin(80)).isEqualTo(1);
    }

    @Test
    void attackerWinDeltaBoundaryAt120() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(119)).isEqualTo(1);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(120)).isEqualTo(2);
    }

    @Test
    void attackerWinDeltaBoundaryAt160() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(159)).isEqualTo(2);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(160)).isEqualTo(3);
    }

    @Test
    void attackerWinDeltaCappedAt200() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(200)).isEqualTo(3);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(240)).isEqualTo(3);
    }

    @Test
    void kittyMultiplierSupportsDefaultPattern() {
        assertThat(ScoreRules.kittyMultiplier("UNKNOWN", 1)).isEqualTo(1);
    }

    @Test
    void advanceLevelRankHandlesZeroDelta() {
        assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 0)).isEqualTo(Rank.ACE);
        assertThat(ScoreRules.advanceLevelRank(Rank.KING, 0)).isEqualTo(Rank.KING);
    }

    @Test
    void advanceLevelRankCapsAtKingEvenWithLargeDelta() {
        assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 20)).isEqualTo(Rank.KING);
    }
}
