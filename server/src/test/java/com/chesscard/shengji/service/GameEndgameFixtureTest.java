package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GameEndgameFixtureTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void allEndgameFixturesLoadAndMatchExpectedOutcome() throws Exception {
        URL resource = getClass().getClassLoader().getResource("fixtures/endgames");
        assertThat(resource).as("fixtures/endgames resource directory").isNotNull();
        List<Path> fixturePaths;
        try (var paths = Files.list(Path.of(resource.toURI()))) {
            fixturePaths = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        assertThat(fixturePaths).hasSizeGreaterThanOrEqualTo(4);

        for (Path fixturePath : fixturePaths) {
            EndgameFixture fixture = OBJECT_MAPPER.readValue(fixturePath.toFile(), EndgameFixture.class);

            FakeGameRepository repository = new FakeGameRepository();
            GameService service = new GameService(repository, new AiPlayer());
            repository.save(fixture.gameState);

            GameState result = service.play(
                    fixture.gameState.getId(),
                    fixture.action.seat,
                    fixture.action.cards);

            assertThat(result.getPhase()).as(fixture.name + " phase").isEqualTo(fixture.expectedPhase);
            assertThat(lastWinner(result)).as(fixture.name + " winner").isEqualTo(fixture.expectedWinner);
            assertThat(result.getAttackerScore()).as(fixture.name + " attacker score")
                    .isEqualTo(fixture.expectedAttackerScore);
            if (fixture.expectedCurrentTurn != null) {
                assertThat(result.getCurrentTurn()).as(fixture.name + " current turn")
                        .isEqualTo(fixture.expectedCurrentTurn);
            }
            if (fixture.expectedLastActionMessage != null) {
                assertThat(result.getLastActionMessage()).as(fixture.name + " last action message")
                        .contains(fixture.expectedLastActionMessage);
            }
        }
    }

    private static PlayerSeat lastWinner(GameState game) {
        if (game.getPlayedTricks().isEmpty()) {
            return null;
        }
        return game.getPlayedTricks().get(game.getPlayedTricks().size() - 1).getWinner();
    }

    private static class EndgameFixture {
        public String name;
        public GameState gameState;
        public FixtureAction action;
        public GamePhase expectedPhase;
        public PlayerSeat expectedWinner;
        public int expectedAttackerScore;
        public PlayerSeat expectedCurrentTurn;
        public String expectedLastActionMessage;
    }

    private static class FixtureAction {
        public PlayerSeat seat;
        public List<Card> cards;
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
