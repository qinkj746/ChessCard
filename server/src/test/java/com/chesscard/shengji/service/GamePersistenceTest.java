package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.persistence.GameSessionEntity;
import com.chesscard.shengji.persistence.GameSessionJpaRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class GamePersistenceTest {
    @Autowired
    private GameService gameService;

    @Autowired
    private GameSessionJpaRepository jpaRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void createGamePersistsSnapshotAndCanLoadItBack() {
        String id = gameService.createGame().getId();

        assertThat(jpaRepository.findById(id)).isPresent();
        assertThat(gameService.getGame(id).getHands().get(com.chesscard.shengji.domain.PlayerSeat.SOUTH)).hasSize(25);
    }

    @Test
    void roomBackedGamePersistsQueryableRoomId() {
        String roomId = "room-persistence-test";
        Map<PlayerSeat, RoomSeat> seats = new EnumMap<>(PlayerSeat.class);
        seats.put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, "player-1", Instant.now()));
        seats.put(PlayerSeat.NORTH, new RoomSeat(PlayerSeat.NORTH, "player-2", Instant.now()));
        seats.put(PlayerSeat.WEST, RoomSeat.bot(PlayerSeat.WEST, Instant.now()));
        seats.put(PlayerSeat.EAST, RoomSeat.bot(PlayerSeat.EAST, Instant.now()));

        GameState game = gameService.createGameForRoom(roomId, seats);

        assertThat(jpaRepository.findById(game.getId()))
                .get()
                .extracting(GameSessionEntity::getRoomId)
                .isEqualTo(roomId);
        assertThat(jpaRepository.findByRoomIdOrderByUpdatedAtDesc(roomId))
                .extracting(GameSessionEntity::getId)
                .contains(game.getId());
        assertThat(gameRepository.findByRoomId(roomId))
                .get()
                .extracting(GameState::getId)
                .isEqualTo(game.getId());
    }
}
