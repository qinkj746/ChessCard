package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameRecord;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GameRecordServiceTest {
    private final FakeGameRecordRepository repository = new FakeGameRecordRepository();
    private final GameRecordService service = new GameRecordService(repository);

    @Test
    void recordFinishedGameCreatesRecordWithSummaryFields() {
        GameState game = finishedGame("game-1", "room-1");

        GameRecord record = service.recordFinishedGame(game);

        assertThat(record.getRecordId()).isNotBlank();
        assertThat(record.getGameId()).isEqualTo("game-1");
        assertThat(record.getRoomId()).isEqualTo("room-1");
        assertThat(record.getStartedAt()).isNotNull();
        assertThat(record.getFinishedAt()).isNotNull();
        assertThat(record.getPlayers()).containsEntry(PlayerSeat.SOUTH, "player-south");
        assertThat(record.getWinningTeam()).isEqualTo(Team.SOUTH_NORTH);
        assertThat(record.getAttackerScore()).isEqualTo(70);
        assertThat(record.getLevelDelta()).isEqualTo(2);
        assertThat(record.getNextLevelRank()).isEqualTo(Rank.THREE);
        assertThat(record.isCompleted()).isFalse();
    }

    @Test
    void recordFinishedGameIsIdempotentForSameGame() {
        GameState game = finishedGame("game-1", "room-1");

        GameRecord first = service.recordFinishedGame(game);
        GameRecord second = service.recordFinishedGame(game);

        assertThat(second.getRecordId()).isEqualTo(first.getRecordId());
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void findRecordsForPlayerReturnsOnlyMatchingPlayers() {
        GameRecord southRecord = service.recordFinishedGame(finishedGame("game-1", "room-1"));
        GameState otherGame = finishedGame("game-2", "room-2");
        otherGame.getSeatOwners().put(PlayerSeat.SOUTH, "other-player");
        otherGame.getSeatOwners().put(PlayerSeat.NORTH, "other-north");
        service.recordFinishedGame(otherGame);

        List<GameRecord> records = service.findRecordsForPlayer("player-south");

        assertThat(records).containsExactly(southRecord);
    }

    private static GameState finishedGame(String gameId, String roomId) {
        GameState game = new GameState();
        game.setId(gameId);
        game.setRoomId(roomId);
        game.setPhase(GamePhase.FINISHED);
        game.setWinningTeam(Team.SOUTH_NORTH);
        game.setAttackerScore(70);
        game.setLevelDelta(2);
        game.setNextLevelRank(Rank.THREE);
        game.setCompleted(false);
        game.getSeatOwners().put(PlayerSeat.SOUTH, "player-south");
        game.getSeatOwners().put(PlayerSeat.WEST, "player-west");
        game.getSeatOwners().put(PlayerSeat.NORTH, "player-north");
        game.getSeatOwners().put(PlayerSeat.EAST, "player-east");
        return game;
    }

    private static class FakeGameRecordRepository implements GameRecordRepository {
        final Map<String, GameRecord> byGameId = new HashMap<>();
        final List<GameRecord> saved = new ArrayList<>();

        @Override
        public GameRecord save(GameRecord record) {
            byGameId.put(record.getGameId(), record);
            saved.add(record);
            return record;
        }

        @Override
        public Optional<GameRecord> findByGameId(String gameId) {
            return Optional.ofNullable(byGameId.get(gameId));
        }

        @Override
        public List<GameRecord> findByPlayerId(String playerId) {
            return byGameId.values().stream()
                    .filter(record -> record.getPlayers().containsValue(playerId))
                    .toList();
        }
    }
}