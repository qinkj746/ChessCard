package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPlayerTest {
    @Test
    void rejectsMissingGameWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();

        assertThatThrownBy(() -> aiPlayer.choosePlay(null, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("牌局状态不能为空");
    }

    @Test
    void rejectsMissingSeatWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();

        assertThatThrownBy(() -> aiPlayer.choosePlay(playingGame(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家座位不能为空");
    }

    @Test
    void rejectsMissingSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().remove(PlayerSeat.WEST);

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌不能为空");
    }

    @Test
    void rejectsNullSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, null);

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌不能为空");
    }

    @Test
    void rejectsNullCardInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(Collections.singletonList(null)));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌不能包含空牌");
    }

    @Test
    void rejectsDuplicateCardInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeFive, spadeFive)));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌不能包含重复牌");
    }

    @Test
    void rejectsMissingSuitForNonJokerCardInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(null, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌普通牌不能缺少花色");
    }

    @Test
    void rejectsMissingRankInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, null, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌不能缺少点数");
    }

    @Test
    void rejectsSuitForJokerInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.HEART, Rank.BIG_JOKER, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌大小王不能包含花色");
    }

    @Test
    void rejectsInvalidDeckIndexInSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 2))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家手牌牌副索引必须为 0 或 1");
    }

    @Test
    void rejectsMissingLeadCardsWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("首攻出牌不能为空");
    }

    @Test
    void rejectsMissingLeadCardsWithEmptyHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("首攻出牌不能为空");
    }

    @Test
    void rejectsNullLeadCardsWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能为空");
    }

    @Test
    void rejectsEmptyLeadCardsWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能为空");
    }

    @Test
    void rejectsNullCardInLeadCardsWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        List<Card> leadCards = new ArrayList<>();
        leadCards.add(card(Suit.SPADE, Rank.SIX, 0));
        leadCards.add(null);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, leadCards);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能包含空牌");
    }

    @Test
    void rejectsNullCardsForOtherCurrentTrickPlayerWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.EAST, null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能为空");
    }

    @Test
    void rejectsEmptyCardsForOtherCurrentTrickPlayerWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.EAST, new ArrayList<>());
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能为空");
    }

    @Test
    void rejectsNullCardForOtherCurrentTrickPlayerWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        List<Card> eastCards = new ArrayList<>();
        eastCards.add(card(Suit.SPADE, Rank.SEVEN, 0));
        eastCards.add(null);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.EAST, eastCards);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌不能包含空牌");
    }

    @Test
    void rejectsSeatAlreadyInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(card(Suit.SPADE, Rank.FIVE, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("玩家已在当前墩出牌");
    }

    @Test
    void rejectsLaterSeatAlreadyInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(card(Suit.SPADE, Rank.SEVEN, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前墩出牌顺序不合法");
    }

    @Test
    void rejectsMissingEarlierSeatWithCardsWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(card(Suit.SPADE, Rank.SEVEN, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.EIGHT, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.EAST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingEarlierSeatHandWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(card(Suit.SPADE, Rank.SEVEN, 0)));
        game.getHands().remove(PlayerSeat.WEST);
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.EIGHT, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.EAST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInsufficientHandCardsForLeadWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMismatchedCardCountForCurrentTrickPlayerWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SIX, 1)));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(card(Suit.SPADE, Rank.FIVE, 0)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.SEVEN, 1))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.EAST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHandCardAlreadyInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadSix));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                leadSix,
                card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateCardInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(spadeSix));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(spadeSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.EAST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingRankInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(card(Suit.SPADE, null, 0)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.EAST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSuitForJokerInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.HEART, Rank.BIG_JOKER, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSuitForNonJokerCardInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(null, Rank.SIX, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidDeckIndexInCurrentTrickWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 2)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.SEVEN, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrickLeaderWhenCurrentTrickHasCards() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(card(Suit.SPADE, Rank.SIX, 0)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingLevelRankWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setLevelRank(null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTrumpSuitWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setTrumpSuit(null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingPhaseWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setPhase(null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPlayPhaseWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setPhase(GamePhase.DECLARE);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSeatThatIsNotCurrentTurnWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTurn(PlayerSeat.SOUTH);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCurrentTurnWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTurn(null);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前行动座位不能为空");
    }

    @Test
    void rejectsHumanSeatWhenChoosingPlay() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        game.setCurrentTurn(PlayerSeat.SOUTH);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(card(Suit.SPADE, Rank.FIVE, 0))));

        assertThatThrownBy(() -> aiPlayer.choosePlay(game, PlayerSeat.SOUTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void attackerUsesLowestWinningTrumpToCapturePoints() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadTen = card(Suit.SPADE, Rank.TEN, 0);
        Card highTrump = card(Suit.HEART, Rank.KING, 0);
        Card lowDiscard = card(Suit.CLUB, Rank.THREE, 0);
        Card lowTrump = card(Suit.HEART, Rank.THREE, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadTen));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(highTrump, lowDiscard, lowTrump)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(lowTrump);
    }

    @Test
    void defenderUsesLowestWinningTrumpToBlockAttackerPoints() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card defenderLead = card(Suit.SPADE, Rank.TEN, 0);
        Card attackerWinner = card(Suit.SPADE, Rank.KING, 0);
        Card highTrump = card(Suit.HEART, Rank.KING, 0);
        Card lowDiscard = card(Suit.CLUB, Rank.FOUR, 0);
        Card lowTrump = card(Suit.HEART, Rank.THREE, 0);
        game.setCurrentTurn(PlayerSeat.NORTH);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(defenderLead));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(attackerWinner));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(highTrump, lowDiscard, lowTrump)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.NORTH);

        assertThat(selected).containsExactly(lowTrump);
    }

    @Test
    void discardsLowestValueCardWhenNoPointsAreAtStake() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadSix = card(Suit.SPADE, Rank.SIX, 0);
        Card trump = card(Suit.HEART, Rank.THREE, 0);
        Card lowDiscard = card(Suit.CLUB, Rank.THREE, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadSix));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(trump, lowDiscard)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(lowDiscard);
    }
    @Test
    void followsSuccessfulThrowWithSameCardCount() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card clubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card clubSix = card(Suit.CLUB, Rank.SIX, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeFive, clubFive, clubSix)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(spadeFive, clubFive);
    }

    @Test
    void followsPairWithPairWhenAvailable() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card spadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        Card spadeSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card spadeSix1 = card(Suit.SPADE, Rank.SIX, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSeven, spadeSix0, spadeSix1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(spadeSix0, spadeSix1);
    }

    @Test
    void followsTractorWithTractorWhenAvailable() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card spadeNine0 = card(Suit.SPADE, Rank.NINE, 0);
        Card spadeNine1 = card(Suit.SPADE, Rank.NINE, 1);
        Card spadeSeven0 = card(Suit.SPADE, Rank.SEVEN, 0);
        Card spadeSeven1 = card(Suit.SPADE, Rank.SEVEN, 1);
        Card spadeEight0 = card(Suit.SPADE, Rank.EIGHT, 0);
        Card spadeEight1 = card(Suit.SPADE, Rank.EIGHT, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeNine0, spadeNine1, spadeSeven0, spadeSeven1, spadeEight0, spadeEight1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(spadeSeven0, spadeSeven1, spadeEight0, spadeEight1);
    }

    @Test
    void findsTractorEvenWhenPairOrderIsInterleaved() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card spadeSeven0 = card(Suit.SPADE, Rank.SEVEN, 0);
        Card spadeSeven1 = card(Suit.SPADE, Rank.SEVEN, 1);
        Card spadeNine0 = card(Suit.SPADE, Rank.NINE, 0);
        Card spadeNine1 = card(Suit.SPADE, Rank.NINE, 1);
        Card spadeEight0 = card(Suit.SPADE, Rank.EIGHT, 0);
        Card spadeEight1 = card(Suit.SPADE, Rank.EIGHT, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeSeven0, spadeSeven1, spadeNine0, spadeNine1, spadeEight0, spadeEight1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(spadeSeven0, spadeSeven1, spadeEight0, spadeEight1);
    }

    @Test
    void followsLongerTractorWithAsManyPairsAsPossible() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card leadSeven0 = card(Suit.SPADE, Rank.SEVEN, 0);
        Card leadSeven1 = card(Suit.SPADE, Rank.SEVEN, 1);
        Card spadeNine0 = card(Suit.SPADE, Rank.NINE, 0);
        Card spadeNine1 = card(Suit.SPADE, Rank.NINE, 1);
        Card spadeTen = card(Suit.SPADE, Rank.TEN, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeJack0 = card(Suit.SPADE, Rank.JACK, 0);
        Card spadeJack1 = card(Suit.SPADE, Rank.JACK, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(
                leadFive0, leadFive1, leadSix0, leadSix1, leadSeven0, leadSeven1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeNine0, spadeNine1, spadeTen, spadeQueen, spadeKing, spadeJack0, spadeJack1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(spadeNine0, spadeNine1, spadeJack0, spadeJack1, spadeTen, spadeQueen);
    }

    @Test
    void beatsNonTrumpTractorWithTrumpTractorWhenVoidLeadSuit() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card clubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card heartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card heartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card heartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card heartSix1 = card(Suit.HEART, Rank.SIX, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                clubThree, clubFour, heartFive0, heartFive1, heartSix0, heartSix1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(heartFive0, heartFive1, heartSix0, heartSix1);
    }

    @Test
    void beatsNonTrumpPairWithTrumpPairWhenVoidLeadSuit() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card heartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card heartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, heartFive0, heartFive1)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(heartFive0, heartFive1);
    }

    @Test
    void beatsNonTrumpSingleWithTrumpSingleWhenVoidLeadSuit() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card heartFive = card(Suit.HEART, Rank.FIVE, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadFive));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, heartFive)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(heartFive);
    }

    @Test
    void beatsSuccessfulThrowWithTrumpCardsWhenVoidLeadSuit() {
        AiPlayer aiPlayer = new AiPlayer();
        GameState game = playingGame();
        Card leadKing = card(Suit.SPADE, Rank.KING, 0);
        Card leadQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card heartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card heartSix = card(Suit.HEART, Rank.SIX, 0);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(leadKing, leadQueen));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, heartFive, heartSix)));

        List<Card> selected = aiPlayer.choosePlay(game, PlayerSeat.WEST);

        assertThat(selected).containsExactly(heartFive, heartSix);
    }

    private static GameState playingGame() {
        GameState game = new GameState();
        game.setPhase(GamePhase.PLAY);
        game.setTrumpSuit(Suit.HEART);
        game.setLevelRank(Rank.ACE);
        game.setCurrentTurn(PlayerSeat.WEST);
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, new ArrayList<>());
        }
        return game;
    }

    private static Card card(Suit suit, Rank rank, int deckIndex) {
        return new Card(suit, rank, deckIndex);
    }
}
