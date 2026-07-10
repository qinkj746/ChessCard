package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.domain.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceNextGameTest {
    @Test
    void createsNextGameFromFinishedGameUsingNextLevelAndWinningTeamBanker() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(Team.EAST_WEST);
        repository.save(previous);

        GameState next = service.createNextGame(previous.getId());

        assertThat(next.getId()).isNotEqualTo(previous.getId());
        assertThat(next.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(next.getLevelRank()).isEqualTo(Rank.TWO);
        assertThat(next.getBanker()).isEqualTo(PlayerSeat.WEST);
        assertThat(next.getAttackerScore()).isZero();
        assertThat(next.getWinningTeam()).isNull();
        assertThat(next.getLevelDelta()).isZero();
        assertThat(next.getNextLevelRank()).isNull();
        assertThat(next.isCompleted()).isFalse();
        assertThat(next.getHands().values()).allSatisfy(hand -> assertThat(hand).hasSize(25));
        assertThat(next.getKitty()).hasSize(8);
    }

    @Test
    void rejectsNextGameWhenPreviousGameIsNotFinished() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setPhase(GamePhase.PLAY);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u672a\u7ed3\u675f");
    }

    @Test
    void rejectsNextGameWhenPreviousGameIsCompleted() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setCompleted(true);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u901a\u5173");
    }

    @Test
    void rejectsNextGameWhenPreviousNextLevelRankIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setWinningTeam(Team.SOUTH_NORTH);
        previous.setNextLevelRank(null);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u4e0b\u4e00\u5c40\u7ea7\u724c");
    }

    @Test
    void rejectsNextGameWhenPreviousWinningTeamIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(null);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u80dc\u65b9");
    }

    @Test
    void rejectsNextGameWhenPreviousLevelDeltaIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(Team.SOUTH_NORTH);
        previous.setLevelDelta(0);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5347\u7ea7\u6b65\u6570");
    }

    @Test
    void rejectsNextGameWhenPreviousLevelRankIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setLevelRank(null);
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(Team.EAST_WEST);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u7ea7\u724c");
    }

    @Test
    void rejectsNextGameWhenPreviousAttackerScoreExceedsTotalPoints() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.setAttackerScore(210);
        previous.setWinningTeam(Team.EAST_WEST);
        previous.setLevelDelta(3);
        previous.setNextLevelRank(Rank.FOUR);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u95f2\u5bb6\u5206");
    }
    @Test
    void rejectsNextGameWhenPreviousBankerIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.setBanker(null);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5e84\u5bb6");
    }

    @Test
    void rejectsNextGameWhenPreviousTrumpSuitIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.setTrumpSuit(null);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u4e3b");
    }

    @Test
    void rejectsNextGameWhenPreviousLevelDeltaDoesNotMatchAttackerScore() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setAttackerScore(0);
        previous.setLevelDelta(1);
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(Team.SOUTH_NORTH);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5347\u7ea7\u6b65\u6570");
    }

    @Test
    void rejectsNextGameWhenPreviousNextLevelRankDoesNotMatchLevelDelta() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setLevelRank(Rank.ACE);
        previous.setLevelDelta(1);
        previous.setNextLevelRank(Rank.THREE);
        previous.setWinningTeam(Team.EAST_WEST);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u4e0b\u4e00\u5c40\u7ea7\u724c");
    }

    @Test
    void rejectsNextGameWhenPreviousWinningTeamDoesNotMatchAttackerScore() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setAttackerScore(90);
        previous.setLevelDelta(1);
        previous.setNextLevelRank(Rank.TWO);
        previous.setWinningTeam(Team.SOUTH_NORTH);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u80dc\u65b9");
    }

    @Test
    void rejectsNextGameWhenKingLevelFinishedGameIsNotMarkedCompleted() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = finishedGame();
        previous.setLevelRank(Rank.KING);
        previous.setAttackerScore(90);
        previous.setLevelDelta(1);
        previous.setNextLevelRank(Rank.KING);
        previous.setWinningTeam(Team.EAST_WEST);
        previous.setCompleted(false);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("K");
    }

    @Test
    void rejectsNextGameWhenPreviousCurrentTurnIsMissing() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.setCurrentTurn(null);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5f53\u524d");
    }

    @Test
    void rejectsNextGameWhenPreviousHandsAreNotEmpty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        Card leftoverCard = card(Suit.SPADE, Rank.FIVE, 0);
        previous.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(leftoverCard)));
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u624b\u724c");

        assertThat(previous.getHands().get(PlayerSeat.SOUTH)).containsExactly(leftoverCard);
    }
    @Test
    void rejectsNextGameWhenPreviousHandsAreMissingSeat() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.getHands().remove(PlayerSeat.NORTH);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u624b\u724c");

        assertThat(previous.getHands()).doesNotContainKey(PlayerSeat.NORTH);
    }


    @Test
    void rejectsNextGameWhenPreviousKittyIsIncomplete() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.getKitty().remove(0);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5e95\u724c");
    }

    @Test
    void rejectsNextGameWhenPreviousCurrentTrickIsNotEmpty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        Card leftoverCard = card(Suit.SPADE, Rank.FIVE, 0);
        previous.getCurrentTrick().put(PlayerSeat.WEST, List.of(leftoverCard));
        previous.setCurrentTrickLeader(PlayerSeat.WEST);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5f53\u524d");

        assertThat(previous.getCurrentTrick()).containsEntry(PlayerSeat.WEST, List.of(leftoverCard));
    }

    @Test
    void rejectsNextGameWhenPreviousCurrentTrickIsNotEmptyWithoutLeader() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        Card leftoverCard = card(Suit.SPADE, Rank.FIVE, 0);
        previous.getCurrentTrick().put(PlayerSeat.WEST, List.of(leftoverCard));
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5f53\u524d");

        assertThat(previous.getCurrentTrick()).containsEntry(PlayerSeat.WEST, List.of(leftoverCard));
        assertThat(previous.getCurrentTrickLeader()).isNull();
    }

    @Test
    void rejectsNextGameWhenPreviousCurrentTrickLeaderRemains() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState previous = validFinishedGame();
        previous.setCurrentTrickLeader(PlayerSeat.WEST);
        repository.save(previous);

        assertThatThrownBy(() -> service.createNextGame(previous.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u9996\u653b\u5ea7\u4f4d");

        assertThat(previous.getCurrentTrickLeader()).isEqualTo(PlayerSeat.WEST);
    }

    private static GameState finishedGame() {
        GameState game = new GameState();
        game.setPhase(GamePhase.FINISHED);
        game.setLevelRank(Rank.ACE);
        game.setBanker(PlayerSeat.SOUTH);
        game.setTrumpSuit(Suit.HEART);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.setAttackerScore(110);
        game.setLevelDelta(1);
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, new ArrayList<>());
        }
        game.getKitty().addAll(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.KING, 0),
                card(Suit.HEART, Rank.FIVE, 0),
                card(Suit.HEART, Rank.TEN, 0),
                card(Suit.HEART, Rank.KING, 0),
                card(Suit.CLUB, Rank.FIVE, 0),
                card(Suit.DIAMOND, Rank.TEN, 0)));
        return game;
    }

    private static GameState validFinishedGame() {
        GameState game = finishedGame();
        game.setWinningTeam(Team.EAST_WEST);
        game.setNextLevelRank(Rank.TWO);
        return game;
    }

    private static Card card(Suit suit, Rank rank, int deckIndex) {
        return new Card(suit, rank, deckIndex);
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
    }
}





