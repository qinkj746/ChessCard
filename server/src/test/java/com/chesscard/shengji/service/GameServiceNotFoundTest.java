package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameNotFoundException;
import com.chesscard.shengji.domain.GameState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceNotFoundTest {
    @Test
    void getGameThrowsNotFoundExceptionWhenMissing() {
        GameService service = new GameService(new EmptyGameRepository(), new AiPlayer());

        assertThatThrownBy(() -> service.getGame("missing"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("牌局不存在");
    }

    private static class EmptyGameRepository implements GameRepository {
        @Override
        public GameState save(GameState game) {
            return game;
        }

        @Override
        public Optional<GameState> find(String id) {
            return Optional.empty();
        }
    }
}
