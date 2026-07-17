package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.domain.Team;
import com.chesscard.shengji.testutil.CardFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServicePlayTest {
    @Test
    void rejectsOffSuitWhenPlayerCanFollowLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card heartKing = card(Suit.HEART, Rank.KING, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSix, heartKing)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(heartKing)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");
    }

    @Test
    void rejectsPlayingDuplicateCardMoreTimesThanHeld() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive, spadeFive)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsNullCardsWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsMissingCurrentPlayerHandWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().remove(PlayerSeat.SOUTH);
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingCurrentTurnWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.setCurrentTurn(null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingSeatWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), null, List.of(spadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingCurrentTurnWhenAiSteppingPlayPhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.setCurrentTurn(null);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isNull();
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.getHands().remove(PlayerSeat.SOUTH);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsNullCardInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getHands().get(PlayerSeat.SOUTH).add(null);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsNull();
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsMissingRankInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, null, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsMissingSuitForNonJokerInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(null, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsSuitForJokerInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsInvalidDeckIndexInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, Rank.FIVE, 2);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsDuplicateCardInSouthHandWhenAiSteppingHumanTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicateCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(duplicateCard, duplicateCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(duplicateCard, duplicateCard);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsMissingTrickLeaderWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(westFive));
        game.setCurrentTrickLeader(null);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).containsEntry(PlayerSeat.WEST, List.of(westFive));
    }

    @Test
    void rejectsMissingLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(westFive));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).containsEntry(PlayerSeat.NORTH, List.of(westFive));
    }

    @Test
    void rejectsEmptyLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of());
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).containsEntry(PlayerSeat.WEST, List.of());
    }

    @Test
    void rejectsNullLeadCardWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        List<Card> leadCards = new ArrayList<>();
        leadCards.add(null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, leadCards);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsNull();
    }

    @Test
    void rejectsMissingRankInLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, null, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(brokenCard);
    }

    @Test
    void rejectsMissingSuitForNonJokerInLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(null, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(brokenCard);
    }

    @Test
    void rejectsSuitForJokerInLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(brokenCard);
    }

    @Test
    void rejectsInvalidDeckIndexInLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, Rank.FIVE, 2);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(brokenCard);
    }

    @Test
    void rejectsDuplicateLeadCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicateCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(duplicateCard, duplicateCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(duplicateCard, duplicateCard);
    }

    @Test
    void rejectsEmptyPlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of());
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick()).containsEntry(PlayerSeat.NORTH, List.of());
    }

    @Test
    void rejectsNullPlayedCardWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        List<Card> northCards = new ArrayList<>();
        northCards.add(null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, northCards);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsNull();
    }

    @Test
    void rejectsMissingRankInPlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenCard = card(Suit.SPADE, null, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(brokenCard);
    }

    @Test
    void rejectsMismatchedPlayedCardCountWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadSix = card(Suit.SPADE, Rank.SIX, 0);
        Card northFive = card(Suit.CLUB, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadFive, leadSix));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northFive));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(northFive);
    }

    @Test
    void rejectsMissingSuitForNonJokerInPlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenCard = card(null, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(brokenCard);
    }

    @Test
    void rejectsSuitForJokerInPlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(brokenCard);
    }

    @Test
    void rejectsInvalidDeckIndexInPlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenCard = card(Suit.SPADE, Rank.SIX, 2);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(brokenCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(brokenCard);
    }

    @Test
    void rejectsDuplicatePlayedCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadSix = card(Suit.SPADE, Rank.SIX, 0);
        Card duplicateCard = card(Suit.CLUB, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadFive, leadSix));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(duplicateCard, duplicateCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(duplicateCard, duplicateCard);
    }

    @Test
    void rejectsCurrentSeatAlreadyPlayedWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card southPlayedCard = card(Suit.CLUB, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southPlayedCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(southPlayedCard);
    }

    @Test
    void rejectsMissingEarlierSeatWithCardsWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card northHandCard = card(Suit.CLUB, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northHandCard)));
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getHands().get(PlayerSeat.NORTH)).containsExactly(northHandCard);
    }

    @Test
    void rejectsMissingEarlierSeatHandWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getHands().remove(PlayerSeat.NORTH);
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(leadCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getHands()).doesNotContainKey(PlayerSeat.NORTH);
    }

    @Test
    void rejectsDuplicateCardAcrossCurrentTrickWhenAiSteppingEmptyHumanHandWithCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicatedCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>());
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>());
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(duplicatedCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(duplicatedCard));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(duplicatedCard);
        assertThat(game.getCurrentTrick().get(PlayerSeat.NORTH)).containsExactly(duplicatedCard);
    }

    @Test
    void rejectsMissingPhaseWhenAiStepping() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.setPhase(null);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isNull();
        assertThat(game.getCurrentTrick()).isEmpty();
    }

    @Test
    void rejectsMissingLevelRankWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        game.setLevelRank(null);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
    }

    @Test
    void rejectsMissingAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        game.getHands().remove(PlayerSeat.WEST);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
    }

    @Test
    void rejectsNullCardInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        List<Card> westHand = new ArrayList<>();
        westHand.add(null);
        game.getHands().put(PlayerSeat.WEST, westHand);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactlyElementsOf(westHand);
    }

    @Test
    void rejectsMissingRankInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        Card brokenCard = card(Suit.HEART, null, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsMissingSuitForNonJokerCardInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        Card brokenCard = card(null, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsSuitOnJokerCardInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        Card brokenCard = card(Suit.SPADE, Rank.BIG_JOKER, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                brokenCard,
                card(Suit.HEART, Rank.ACE, 0)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(brokenCard, card(Suit.HEART, Rank.ACE, 0));
    }

    @Test
    void rejectsInvalidDeckIndexInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        Card brokenCard = card(Suit.HEART, Rank.ACE, 2);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsDuplicatePhysicalCardsInAiHandWhenAiSteppingDeclarePhase() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.DECLARE);
        Card duplicateCard = card(Suit.HEART, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                duplicateCard,
                duplicateCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(card(null, Rank.BIG_JOKER, 0), duplicateCard, duplicateCard);
    }

    @Test
    void rejectsInvalidDeckIndexWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 2);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsNullCardWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        List<Card> selectedCards = new ArrayList<>();
        selectedCards.add(null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingRankWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, null, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(brokenCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingSuitForNonJokerCardWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(null, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(brokenCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsSuitForJokerWhenPlaying() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(brokenCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(brokenCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenCard);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsDuplicateCardWhenPlayingEvenIfHandContainsDuplicate() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        List<Card> selectedCards = new ArrayList<>(List.of(spadeFive, spadeFive));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsEmptyLeadCardsWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of());
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).isEmpty();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsNullLeadCardWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        List<Card> leadCards = new ArrayList<>();
        leadCards.add(null);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, leadCards);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsNull();
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsMissingRankInLeadCardsWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenLeadCard = card(Suit.SPADE, null, 0);
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(brokenLeadCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(brokenLeadCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsMissingSuitForNonJokerLeadCardWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenLeadCard = card(null, Rank.FIVE, 0);
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(brokenLeadCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(brokenLeadCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsSuitForJokerLeadCardWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenLeadCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(brokenLeadCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(brokenLeadCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsInvalidDeckIndexInLeadCardsWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card brokenLeadCard = card(Suit.SPADE, Rank.FIVE, 2);
        Card westCard = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(brokenLeadCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(brokenLeadCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westCard);
    }

    @Test
    void rejectsDuplicateLeadCardsWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicateLeadCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card westFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card westSix = card(Suit.CLUB, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(duplicateLeadCard, duplicateLeadCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westFive, westSix)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westFive, westSix)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(duplicateLeadCard, duplicateLeadCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westFive, westSix);
    }

    @Test
    void rejectsPlayedCardAlreadyInCurrentTrickWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicatedCard = card(Suit.SPADE, Rank.FIVE, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(duplicatedCard));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(duplicatedCard)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(duplicatedCard)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(duplicatedCard);
        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(duplicatedCard);
    }

    @Test
    void rejectsEmptyEarlierPlayedCardsWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of());
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsNullEarlierPlayedCardWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        List<Card> westCards = new ArrayList<>();
        westCards.add(null);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, westCards);
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsMissingRankInEarlierPlayedCardsWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenWestCard = card(Suit.SPADE, null, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenWestCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsMismatchedEarlierPlayedCardCountWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card southSix = card(Suit.SPADE, Rank.SIX, 0);
        Card westFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card northFive = card(Suit.SPADE, Rank.FIVE, 1);
        Card northSix = card(Suit.SPADE, Rank.SIX, 1);
        Card eastFive = card(Suit.HEART, Rank.FIVE, 0);
        Card eastSix = card(Suit.HEART, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive, southSix));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(westFive));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northFive, northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastFive, eastSix)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastFive, eastSix)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastFive, eastSix);
    }

    @Test
    void rejectsMissingSuitForNonJokerEarlierPlayedCardWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenWestCard = card(null, Rank.SIX, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenWestCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsSuitForJokerEarlierPlayedCardWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenWestCard = card(Suit.SPADE, Rank.SMALL_JOKER, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenWestCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsInvalidDeckIndexInEarlierPlayedCardsWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card brokenWestCard = card(Suit.SPADE, Rank.SIX, 2);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(southFive));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(brokenWestCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsDuplicateCardAcrossExistingCurrentTrickWhenPlayingLastFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card duplicatedCard = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSix = card(Suit.SPADE, Rank.SIX, 0);
        Card eastSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, List.of(duplicatedCard));
        game.getCurrentTrick().put(PlayerSeat.WEST, List.of(duplicatedCard));
        game.getCurrentTrick().put(PlayerSeat.NORTH, List.of(northSix));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSeven)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSeven)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTrick()).doesNotContainKey(PlayerSeat.EAST);
        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSeven);
    }

    @Test
    void rejectsMissingLeadCardsWhenPlayingFollower() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northSpadeSix)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westSpadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westSpadeFive);
        assertThat(game.getCurrentTrick()).containsOnlyKeys(PlayerSeat.NORTH);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingLeadCardsBeforeNormalizingInvalidFollowerPlay() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card westClubSix = card(Suit.CLUB, Rank.SIX, 0);
        Card northSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northSpadeSix)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive, westClubSix)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westSpadeFive, westClubSix)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westSpadeFive, westClubSix);
        assertThat(game.getCurrentTrick()).containsOnlyKeys(PlayerSeat.NORTH);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingTrickLeaderWhenCurrentTrickIsNotEmpty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card westSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(null);
        game.getCurrentTrick().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSix)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westSpadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westSpadeFive);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westSpadeSix);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsSeatThatAlreadyPlayedInCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card westSpadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southSpadeSix)));
        game.getCurrentTrick().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSeven)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westSpadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westSpadeFive);
        assertThat(game.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westSpadeSeven);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsLaterSeatAlreadyPlayedBeforeCurrentSeat() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSpadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southSpadeSix)));
        game.getCurrentTrick().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northSpadeSeven)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westSpadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.WEST)).containsExactly(westSpadeFive);
        assertThat(game.getCurrentTrick()).containsOnlyKeys(PlayerSeat.SOUTH, PlayerSeat.NORTH);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void rejectsMissingEarlierSeatWithCardsWhenPlayingLaterSeat() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card westSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card northSpadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        Card eastSpadeEight = card(Suit.SPADE, Rank.EIGHT, 0);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.setCurrentTrickLeader(PlayerSeat.SOUTH);
        game.getCurrentTrick().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southSpadeSix)));
        game.getCurrentTrick().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northSpadeSeven)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeFive)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSpadeEight)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.EAST, List.of(eastSpadeEight)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.EAST)).containsExactly(eastSpadeEight);
        assertThat(game.getCurrentTrick()).containsOnlyKeys(PlayerSeat.SOUTH, PlayerSeat.NORTH);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PLAY);
    }

    @Test
    void unsupportedThrowDegradesToLowestSingleCard() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSix)));
        repository.save(game);

        GameState next = service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeFive));

        assertThat(next.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(next.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeKing);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
    }

    @Test
    void successfulThrowPlaysAllCardsWhenEveryCardIsHighest() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(card(Suit.SPADE, Rank.TEN, 0))));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(card(Suit.SPADE, Rank.JACK, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.CLUB, Rank.KING, 0))));
        repository.save(game);

        GameState next = service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));

        assertThat(next.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(spadeKing, spadeQueen);
        assertThat(next.getHands().get(PlayerSeat.SOUTH)).isEmpty();
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
    }

    @Test
    void aiStepCanFollowSuccessfulThrow() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card clubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card clubSix = card(Suit.CLUB, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeFive, clubFive, clubSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(card(Suit.CLUB, Rank.SEVEN, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.DIAMOND, Rank.SEVEN, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(spadeFive, clubFive);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void aiStepCanBeatSuccessfulThrowWithTrumpCardsWhenVoidLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card westHeartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartSix = card(Suit.HEART, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, westHeartFive, westHeartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(card(Suit.CLUB, Rank.FIVE, 0), card(Suit.CLUB, Rank.SIX, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.DIAMOND, Rank.FIVE, 0), card(Suit.DIAMOND, Rank.SIX, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westHeartFive, westHeartSix);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void aiStepTrumpingSuccessfulThrowSettlesTrickForAi() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card westClubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card westHeartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartSix = card(Suit.HEART, Rank.SIX, 0);
        Card northClubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card northClubSix = card(Suit.CLUB, Rank.SIX, 0);
        Card eastDiamondFive = card(Suit.DIAMOND, Rank.FIVE, 0);
        Card eastDiamondSix = card(Suit.DIAMOND, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westClubThree, westHeartFive, westHeartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northClubFive, northClubSix)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastDiamondFive, eastDiamondSix)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.aiStep(game.getId());

        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(25);
    }

    @Test
    void aiStepFollowsPairWithPairWhenAvailable() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card spadeSeven = card(Suit.SPADE, Rank.SEVEN, 0);
        Card spadeSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card spadeSix1 = card(Suit.SPADE, Rank.SIX, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSeven, spadeSix0, spadeSix1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(card(Suit.CLUB, Rank.SEVEN, 0), card(Suit.CLUB, Rank.EIGHT, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.DIAMOND, Rank.SEVEN, 0), card(Suit.DIAMOND, Rank.EIGHT, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(spadeSix0, spadeSix1);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void repeatedAiStepSettlesTrickAfterHumanLead() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card westSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card northSpadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card eastSpadeTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northSpadeKing)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastSpadeTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.aiStep(game.getId());

        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
        assertThat(settled.getAttackerScore()).isZero();
    }

    @Test
    void aiStepFollowsTractorWithTractorWhenAvailable() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
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
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeNine0, spadeNine1, spadeSeven0, spadeSeven1, spadeEight0, spadeEight1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                card(Suit.CLUB, Rank.SEVEN, 0), card(Suit.CLUB, Rank.EIGHT, 0),
                card(Suit.CLUB, Rank.NINE, 0), card(Suit.CLUB, Rank.TEN, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                card(Suit.DIAMOND, Rank.SEVEN, 0), card(Suit.DIAMOND, Rank.EIGHT, 0),
                card(Suit.DIAMOND, Rank.NINE, 0), card(Suit.DIAMOND, Rank.TEN, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST))
                .containsExactly(spadeSeven0, spadeSeven1, spadeEight0, spadeEight1);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void aiStepFollowsLongerTractorWithAsManyPairsAsPossible() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
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
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                leadFive0, leadFive1, leadSix0, leadSix1, leadSeven0, leadSeven1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeNine0, spadeNine1, spadeTen, spadeQueen, spadeKing, spadeJack0, spadeJack1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                card(Suit.CLUB, Rank.SEVEN, 0), card(Suit.CLUB, Rank.EIGHT, 0),
                card(Suit.CLUB, Rank.NINE, 0), card(Suit.CLUB, Rank.TEN, 0),
                card(Suit.CLUB, Rank.JACK, 0), card(Suit.CLUB, Rank.QUEEN, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                card(Suit.DIAMOND, Rank.SEVEN, 0), card(Suit.DIAMOND, Rank.EIGHT, 0),
                card(Suit.DIAMOND, Rank.NINE, 0), card(Suit.DIAMOND, Rank.TEN, 0),
                card(Suit.DIAMOND, Rank.JACK, 0), card(Suit.DIAMOND, Rank.QUEEN, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(
                leadFive0, leadFive1, leadSix0, leadSix1, leadSeven0, leadSeven1));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST))
                .containsExactly(spadeNine0, spadeNine1, spadeJack0, spadeJack1, spadeTen, spadeQueen);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void trumpCardsCanBeatSuccessfulThrowWhenFollowerHasNoLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card heartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card heartSix = card(Suit.HEART, Rank.SIX, 0);
        Card clubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card clubSix = card(Suit.CLUB, Rank.SIX, 0);
        Card diamondFive = card(Suit.DIAMOND, Rank.FIVE, 0);
        Card diamondSix = card(Suit.DIAMOND, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(heartFive, heartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(clubFive, clubSix)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(diamondFive, diamondSix)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        service.play(game.getId(), PlayerSeat.WEST, List.of(heartFive, heartSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(clubFive, clubSix));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(diamondFive, diamondSix));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(25);
    }

    @Test
    void rejectsTrumpingSuccessfulThrowWhenFollowerCanFollowLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card heartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card heartSix = card(Suit.HEART, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeFive, heartFive, heartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(card(Suit.CLUB, Rank.FIVE, 0))));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(card(Suit.DIAMOND, Rank.FIVE, 0))));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(heartFive, heartSix)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void higherTrumpCardsWinWhenMultiplePlayersBeatSuccessfulThrow() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card westHeartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartSix = card(Suit.HEART, Rank.SIX, 0);
        Card northHeartKing = card(Suit.HEART, Rank.KING, 0);
        Card northHeartQueen = card(Suit.HEART, Rank.QUEEN, 0);
        Card eastClubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card eastClubSix = card(Suit.CLUB, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westHeartFive, westHeartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northHeartKing, northHeartQueen)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastClubFive, eastClubSix)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive, westHeartSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(northHeartKing, northHeartQueen));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(eastClubFive, eastClubSix));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
        assertThat(settled.getAttackerScore()).isZero();
    }

    @Test
    void trumpTractorBeatsNonTrumpTractorWhenFollowerHasNoLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card westHeartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card westHeartSix1 = card(Suit.HEART, Rank.SIX, 1);
        Card northClubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card northClubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card northClubSeven = card(Suit.CLUB, Rank.SEVEN, 0);
        Card northClubEight = card(Suit.CLUB, Rank.EIGHT, 0);
        Card eastDiamondThree = card(Suit.DIAMOND, Rank.THREE, 0);
        Card eastDiamondFour = card(Suit.DIAMOND, Rank.FOUR, 0);
        Card eastDiamondSeven = card(Suit.DIAMOND, Rank.SEVEN, 0);
        Card eastDiamondEight = card(Suit.DIAMOND, Rank.EIGHT, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                northClubThree, northClubFour, northClubSeven, northClubEight)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                eastDiamondThree, eastDiamondFour, eastDiamondSeven, eastDiamondEight)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(
                westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(
                northClubThree, northClubFour, northClubSeven, northClubEight));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(
                eastDiamondThree, eastDiamondFour, eastDiamondSeven, eastDiamondEight));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(20);
    }

    @Test
    void aiStepBeatsNonTrumpTractorWithTrumpTractorWhenVoidLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card clubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card westHeartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card westHeartSix1 = card(Suit.HEART, Rank.SIX, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                clubThree, clubFour, westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST))
                .containsExactly(westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void aiStepBeatsNonTrumpPairWithTrumpPairWhenVoidLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, westHeartFive0, westHeartFive1)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westHeartFive0, westHeartFive1);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void trumpPairBeatsNonTrumpPairWhenFollowerHasNoLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card northClubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card northClubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card eastDiamondThree = card(Suit.DIAMOND, Rank.THREE, 0);
        Card eastDiamondFour = card(Suit.DIAMOND, Rank.FOUR, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westHeartFive0, westHeartFive1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northClubThree, northClubFour)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastDiamondThree, eastDiamondFour)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive0, westHeartFive1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(northClubThree, northClubFour));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(eastDiamondThree, eastDiamondFour));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(20);
    }

    @Test
    void higherTrumpPairWinsWhenMultiplePlayersTrumpNonTrumpPair() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card northHeartKing0 = card(Suit.HEART, Rank.KING, 0);
        Card northHeartKing1 = card(Suit.HEART, Rank.KING, 1);
        Card eastDiamondThree = card(Suit.DIAMOND, Rank.THREE, 0);
        Card eastDiamondFour = card(Suit.DIAMOND, Rank.FOUR, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westHeartFive0, westHeartFive1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northHeartKing0, northHeartKing1)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastDiamondThree, eastDiamondFour)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive0, westHeartFive1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(northHeartKing0, northHeartKing1));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(eastDiamondThree, eastDiamondFour));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
        assertThat(settled.getAttackerScore()).isZero();
    }

    @Test
    void rejectsTrumpingPairWhenFollowerCanFollowLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card spadeThree = card(Suit.SPADE, Rank.THREE, 0);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeThree, westHeartFive0, westHeartFive1)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1));

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive0, westHeartFive1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTrumpingTractorWhenFollowerCanFollowLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card spadeThree = card(Suit.SPADE, Rank.THREE, 0);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card westHeartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card westHeartSix1 = card(Suit.HEART, Rank.SIX, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                spadeThree, westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(
                westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void higherTrumpTractorWinsWhenMultiplePlayersTrumpNonTrumpTractor() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card westHeartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card westHeartSix1 = card(Suit.HEART, Rank.SIX, 1);
        Card northHeartSeven0 = card(Suit.HEART, Rank.SEVEN, 0);
        Card northHeartSeven1 = card(Suit.HEART, Rank.SEVEN, 1);
        Card northHeartEight0 = card(Suit.HEART, Rank.EIGHT, 0);
        Card northHeartEight1 = card(Suit.HEART, Rank.EIGHT, 1);
        Card eastDiamondThree = card(Suit.DIAMOND, Rank.THREE, 0);
        Card eastDiamondFour = card(Suit.DIAMOND, Rank.FOUR, 0);
        Card eastDiamondFive = card(Suit.DIAMOND, Rank.FIVE, 0);
        Card eastDiamondSix = card(Suit.DIAMOND, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                northHeartSeven0, northHeartSeven1, northHeartEight0, northHeartEight1)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                eastDiamondThree, eastDiamondFour, eastDiamondFive, eastDiamondSix)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive0, westHeartFive1, westHeartSix0, westHeartSix1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(
                northHeartSeven0, northHeartSeven1, northHeartEight0, northHeartEight1));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(
                eastDiamondThree, eastDiamondFour, eastDiamondFive, eastDiamondSix));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
        assertThat(settled.getAttackerScore()).isZero();
    }

    @Test
    void aiStepBeatsNonTrumpSingleWithTrumpSingleWhenVoidLeadSuit() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card clubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card westHeartFive = card(Suit.HEART, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(clubThree, westHeartFive)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive));
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westHeartFive);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void attackerLastTrickTrumpingThrowAddsKittyPointsWithThrowCardCountMultiplier() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card heartFive = card(Suit.HEART, Rank.FIVE, 0);
        Card heartSix = card(Suit.HEART, Rank.SIX, 0);
        Card clubFive = card(Suit.CLUB, Rank.FIVE, 0);
        Card clubSix = card(Suit.CLUB, Rank.SIX, 0);
        Card diamondFive = card(Suit.DIAMOND, Rank.FIVE, 0);
        Card diamondSix = card(Suit.DIAMOND, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeKing, spadeQueen)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(heartFive, heartSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(clubFive, clubSix)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(diamondFive, diamondSix)));
        game.getKitty().add(card(Suit.CLUB, Rank.KING, 0));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeKing, spadeQueen));
        service.play(game.getId(), PlayerSeat.WEST, List.of(heartFive, heartSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(clubFive, clubSix));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(diamondFive, diamondSix));

        assertThat(settled.getPhase()).isEqualTo(GamePhase.FINISHED);
        assertThat(settled.getAttackerScore()).isEqualTo(65);
    }

    @Test
    void settlesTrickWinnerAndAwardsPointsToAttackersOnlyWhenTheyWin() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card heartThree = card(Suit.HEART, Rank.THREE, 0);
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(heartThree)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(spadeKing)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(spadeTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.play(game.getId(), PlayerSeat.WEST, List.of(heartThree));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(spadeKing));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(spadeTen));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getAttackerScore()).isEqualTo(25);
    }

    @Test
    void finishesGameAfterLastTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(spadeKing)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(spadeTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.play(game.getId(), PlayerSeat.WEST, List.of(spadeSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(spadeKing));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(spadeTen));

        assertThat(settled.getPhase()).isEqualTo(GamePhase.FINISHED);
        assertThat(settled.getWinningTeam()).isEqualTo(Team.SOUTH_NORTH);
        assertThat(settled.getLevelDelta()).isEqualTo(3);
        assertThat(settled.getNextLevelRank()).isEqualTo(Rank.FIVE);
    }

    @Test
    void aiStepSettlesLastTrickAfterAllEmptyAiSeatsSkip() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.aiStep(game.getId());

        assertThat(settled.getPhase()).isEqualTo(GamePhase.FINISHED);
        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getWinningTeam()).isEqualTo(Team.SOUTH_NORTH);
    }

    @Test
    void playSettlesTrickAfterEmptySeatsSkipAndHumanFollowsAiLead() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card southSpadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSix)));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southSpadeFive)));
        repository.save(game);

        service.aiStep(game.getId());
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.play(game.getId(), PlayerSeat.SOUTH, List.of(southSpadeFive));

        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(5);
    }

    @Test
    void aiStepSkipsEmptyHumanSeatAndSettlesCurrentTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSix)));
        repository.save(game);

        service.aiStep(game.getId());
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.aiStep(game.getId());

        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isZero();
    }

    @Test
    void aiStepSkipsEmptyHumanLeadAndLetsNextAiStartTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card westSpadeSix = card(Suit.SPADE, Rank.SIX, 0);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSpadeSix)));
        repository.save(game);

        service.aiStep(game.getId());
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westSpadeSix);
        assertThat(next.getCurrentTrickLeader()).isEqualTo(PlayerSeat.WEST);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    @Test
    void rejectsStaleTrickLeaderWhenStartingNewTrick() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getCurrentTrickLeader()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
    }

    @Test
    void rejectsStaleTrickLeaderWhenAiSteppingEmptyHumanLead() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.setCurrentTrickLeader(PlayerSeat.WEST);
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("");

        assertThat(game.getCurrentTrick()).isEmpty();
        assertThat(game.getCurrentTrickLeader()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.SOUTH);
    }

    @Test
    void attackerLastTrickAddsKittyPointsWithPlayedPatternMultiplier() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card southFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card westKing0 = card(Suit.SPADE, Rank.KING, 0);
        Card westKing1 = card(Suit.SPADE, Rank.KING, 1);
        Card northTen0 = card(Suit.SPADE, Rank.TEN, 0);
        Card northTen1 = card(Suit.SPADE, Rank.TEN, 1);
        Card eastQueen0 = card(Suit.SPADE, Rank.QUEEN, 0);
        Card eastQueen1 = card(Suit.SPADE, Rank.QUEEN, 1);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southFive0, southFive1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westKing0, westKing1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northTen0, northTen1)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastQueen0, eastQueen1)));
        game.getKitty().addAll(List.of(
                card(Suit.CLUB, Rank.FIVE, 0),
                card(Suit.CLUB, Rank.KING, 0)
        ));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(southFive0, southFive1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(westKing0, westKing1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(northTen0, northTen1));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(eastQueen0, eastQueen1));

        assertThat(settled.getPhase()).isEqualTo(GamePhase.FINISHED);
        assertThat(settled.getAttackerScore()).isEqualTo(110);
        assertThat(settled.getWinningTeam()).isEqualTo(Team.EAST_WEST);
        assertThat(settled.getLevelDelta()).isEqualTo(1);
        assertThat(settled.getNextLevelRank()).isEqualTo(Rank.THREE);
    }

    @Test
    void winningAtKingLevelCompletesGame() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.setLevelRank(Rank.KING);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(spadeKing)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(spadeTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.play(game.getId(), PlayerSeat.WEST, List.of(spadeSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(spadeKing));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(spadeTen));

        assertThat(settled.isCompleted()).isTrue();
        assertThat(settled.getNextLevelRank()).isEqualTo(Rank.KING);
    }

    @Test
    void tractorRequiresSameLengthToBeat() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card westHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card westHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card northClubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card northClubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card northClubSeven = card(Suit.CLUB, Rank.SEVEN, 0);
        Card northClubEight = card(Suit.CLUB, Rank.EIGHT, 0);
        Card eastDiamondThree = card(Suit.DIAMOND, Rank.THREE, 0);
        Card eastDiamondFour = card(Suit.DIAMOND, Rank.FOUR, 0);
        Card eastDiamondSeven = card(Suit.DIAMOND, Rank.SEVEN, 0);
        Card eastDiamondEight = card(Suit.DIAMOND, Rank.EIGHT, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westHeartFive0, westHeartFive1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                northClubThree, northClubFour, northClubSeven, northClubEight)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                eastDiamondThree, eastDiamondFour, eastDiamondSeven, eastDiamondEight)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));

        assertThatThrownBy(() -> service.play(game.getId(), PlayerSeat.WEST, List.of(westHeartFive0, westHeartFive1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trumpTractorBeatsNonTrumpTractorWithHigherWeight() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card leadFive0 = card(Suit.SPADE, Rank.FIVE, 0);
        Card leadFive1 = card(Suit.SPADE, Rank.FIVE, 1);
        Card leadSix0 = card(Suit.SPADE, Rank.SIX, 0);
        Card leadSix1 = card(Suit.SPADE, Rank.SIX, 1);
        Card westHeartSeven0 = card(Suit.HEART, Rank.SEVEN, 0);
        Card westHeartSeven1 = card(Suit.HEART, Rank.SEVEN, 1);
        Card westHeartEight0 = card(Suit.HEART, Rank.EIGHT, 0);
        Card westHeartEight1 = card(Suit.HEART, Rank.EIGHT, 1);
        Card northHeartFive0 = card(Suit.HEART, Rank.FIVE, 0);
        Card northHeartFive1 = card(Suit.HEART, Rank.FIVE, 1);
        Card northHeartSix0 = card(Suit.HEART, Rank.SIX, 0);
        Card northHeartSix1 = card(Suit.HEART, Rank.SIX, 1);
        Card eastClubThree = card(Suit.CLUB, Rank.THREE, 0);
        Card eastClubFour = card(Suit.CLUB, Rank.FOUR, 0);
        Card eastClubSeven = card(Suit.CLUB, Rank.SEVEN, 0);
        Card eastClubEight = card(Suit.CLUB, Rank.EIGHT, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leadFive0, leadFive1, leadSix0, leadSix1)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                westHeartSeven0, westHeartSeven1, westHeartEight0, westHeartEight1)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                northHeartFive0, northHeartFive1, northHeartSix0, northHeartSix1)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                eastClubThree, eastClubFour, eastClubSeven, eastClubEight)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(leadFive0, leadFive1, leadSix0, leadSix1));
        service.play(game.getId(), PlayerSeat.WEST, List.of(
                westHeartSeven0, westHeartSeven1, westHeartEight0, westHeartEight1));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(
                northHeartFive0, northHeartFive1, northHeartSix0, northHeartSix1));
        GameState settled = service.play(game.getId(), PlayerSeat.EAST, List.of(
                eastClubThree, eastClubFour, eastClubSeven, eastClubEight));

        assertThat(settled.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(settled.getAttackerScore()).isEqualTo(20);
    }

    @Test
    void normalPlayClearsLastActionMessage() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        game.setLastActionMessage("previous message");
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        repository.save(game);

        GameState result = service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));

        assertThat(result.getLastActionMessage()).isNull();
    }

    @Test
    void failedThrowSetsLastActionMessage() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card heartKing = card(Suit.HEART, Rank.KING, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive, heartKing)));
        repository.save(game);

        GameState result = service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive, heartKing));

        assertThat(result.getLastActionMessage()).isNotBlank();
        assertThat(result.getCurrentTrick().get(PlayerSeat.SOUTH)).containsExactly(spadeFive);
    }


    @Test
    void aiStepCompletingTrickRecordsOrderedPlayedHistory() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southKing = card(Suit.SPADE, Rank.KING, 0);
        Card westQueen = card(Suit.SPADE, Rank.QUEEN, 0);
        Card northJack = card(Suit.SPADE, Rank.JACK, 0);
        Card eastTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southKing)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westQueen)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(northJack)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(eastTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(southKing));
        service.aiStep(game.getId());
        service.aiStep(game.getId());
        GameState settled = service.aiStep(game.getId());

        assertThat(settled.getCurrentTrick()).isEmpty();
        assertThat(settled.getPlayedTricks()).hasSize(1);
        GameState.PlayedTrick trick = settled.getPlayedTricks().get(0);
        assertThat(trick.getIndex()).isEqualTo(1);
        assertThat(trick.getLeader()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(trick.getWinner()).isEqualTo(PlayerSeat.SOUTH);
        assertThat(trick.getPlays()).extracting(GameState.TrickPlay::getSeat)
                .containsExactly(PlayerSeat.SOUTH, PlayerSeat.WEST, PlayerSeat.NORTH, PlayerSeat.EAST);
        assertThat(trick.getPlays().get(3).getCards()).containsExactly(eastTen);
    }

    @Test
    void aiStepPlaysAiSeatInRoomModeWithoutPlayerId() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = playingGame();
        Card southFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card westSix = card(Suit.SPADE, Rank.SIX, 0);
        game.setRoomId("room-1");
        game.getSeatOwners().put(PlayerSeat.SOUTH, "player-1");
        game.getSeatOwners().put(PlayerSeat.WEST, null);
        game.getSeatOwners().put(PlayerSeat.NORTH, null);
        game.getSeatOwners().put(PlayerSeat.EAST, null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(southFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(westSix)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(southFive), "player-1");
        GameState next = service.aiStep(game.getId());

        assertThat(next.getCurrentTrick().get(PlayerSeat.WEST)).containsExactly(westSix);
        assertThat(next.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
    }

    private static GameState playingGame() {
        GameState game = new GameState();
        game.setPhase(GamePhase.PLAY);
        game.setTrumpSuit(Suit.HEART);
        game.setLevelRank(Rank.ACE);
        game.setBanker(PlayerSeat.SOUTH);
        game.setCurrentTurn(PlayerSeat.SOUTH);
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, new ArrayList<>());
        }
        return game;
    }

    private static Card card(Suit suit, Rank rank, int deckIndex) {
        return CardFixtures.card(suit, rank, deckIndex);
    }

    private static class FakeGameRepository implements GameRepository {
        private final Map<String, GameState> games = new java.util.HashMap<>();

        @Override
        public GameState save(GameState game) {
            games.put(game.getId(), game);
            return game;
        }

        @Override
        public Optional<GameState> find(String id) {
            return Optional.ofNullable(games.get(id));
        }

        @Override
        public Optional<GameState> findByRoomId(String roomId) {
            return Optional.empty();
        }
    }
}
