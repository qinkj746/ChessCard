package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.GameRecordDto;
import com.chesscard.shengji.domain.GameRecord;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Team;
import com.chesscard.shengji.service.GameRecordService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameRecordControllerTest {
    @Test
    void playerRecordsReturnsRecordDtos() {
        GameRecordController controller = new GameRecordController(new StubGameRecordService());

        List<GameRecordDto> records = controller.playerRecords("player-south");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).gameId()).isEqualTo("game-1");
        assertThat(records.get(0).players()).containsEntry("SOUTH", "player-south");
    }

    @Test
    void gameRecordReturnsRecordDto() {
        GameRecordController controller = new GameRecordController(new StubGameRecordService());

        GameRecordDto record = controller.gameRecord("game-1");

        assertThat(record.recordId()).isEqualTo("record-1");
        assertThat(record.winningTeam()).isEqualTo("SOUTH_NORTH");
        assertThat(record.nextLevelRank()).isEqualTo("THREE");
    }

    @Test
    void missingRecordMapsToNotFound() {
        GameRecordController controller = new GameRecordController(new StubGameRecordService());

        var response = controller.notFound(new GameRecordNotFoundException("record missing"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo("GAME_RECORD_NOT_FOUND");
    }

    private static class StubGameRecordService extends GameRecordService {
        StubGameRecordService() {
            super(null);
        }

        @Override
        public List<GameRecord> findRecordsForPlayer(String playerId) {
            return List.of(record());
        }

        @Override
        public GameRecord getRecordForGame(String gameId) {
            return record();
        }

        private GameRecord record() {
            return new GameRecord(
                    "record-1",
                    "game-1",
                    "room-1",
                    Instant.parse("2026-07-10T08:00:00Z"),
                    Instant.parse("2026-07-10T08:30:00Z"),
                    Map.of(PlayerSeat.SOUTH, "player-south"),
                    Team.SOUTH_NORTH,
                    70,
                    2,
                    Rank.THREE,
                    false
            );
        }
    }
}