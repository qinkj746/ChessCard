package com.chesscard.shengji.api;

import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.service.AiPlayer;
import com.chesscard.shengji.service.GameRepository;
import com.chesscard.shengji.service.GameService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GameControllerNextGameTest {
    @Test
    void nextGameReturnsCreatedGameState() {
        GameService service = new GameService(new FakeGameRepository(), new AiPlayer());
        GameState previous = service.createGame();
        previous.setPhase(com.chesscard.shengji.domain.GamePhase.FINISHED);
        previous.setBanker(com.chesscard.shengji.domain.PlayerSeat.SOUTH);
        previous.setTrumpSuit(com.chesscard.shengji.domain.Suit.SPADE);
        previous.setCurrentTurn(com.chesscard.shengji.domain.PlayerSeat.SOUTH);
        previous.setAttackerScore(110);
        previous.setWinningTeam(com.chesscard.shengji.domain.Team.EAST_WEST);
        previous.setLevelDelta(1);
        previous.setNextLevelRank(Rank.TWO);
        previous.getHands().values().forEach(java.util.List::clear);
        GameController controller = new GameController(service);

        var next = controller.next(previous.getId());

        assertThat(next.id()).isNotEqualTo(previous.getId());
        assertThat(next.levelRank()).isEqualTo(Rank.TWO);
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
