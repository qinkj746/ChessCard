package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceTest {
    private final FakeRoomRepository roomRepo = new FakeRoomRepository();
    private final FakeGameRepository gameRepo = new FakeGameRepository();
    private final GameService gameService = new GameService(gameRepo, new AiPlayer());
    private final RoomService service = new RoomService(roomRepo, gameService);

    @Test
    void createsRoomWithOwnerOnSouthSeat() {
        RoomState room = service.createRoom("player-1");

        assertThat(room.getRoomId()).isNotBlank();
        assertThat(room.getPhase()).isEqualTo(RoomPhase.WAITING);
        assertThat(room.getVersion()).isEqualTo(1);
        assertThat(room.getOwnerPlayerId()).isEqualTo("player-1");
        assertThat(room.getSeats()).containsKey(PlayerSeat.SOUTH);
        assertThat(room.getSeats().get(PlayerSeat.SOUTH).getPlayerId()).isEqualTo("player-1");
        assertThat(room.getGameId()).isNull();
    }

    @Test
    void rejectsBlankOwnerPlayerId() {
        assertThatThrownBy(() -> service.createRoom(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createRoom("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createRoom(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roomIsPersistedAfterCreation() {
        RoomState room = service.createRoom("player-1");

        assertThat(roomRepo.store).containsKey(room.getRoomId());
    }

    @Test
    void getRoomReturnsExistingRoom() {
        RoomState created = service.createRoom("player-1");
        RoomState found = service.getRoom(created.getRoomId());

        assertThat(found.getRoomId()).isEqualTo(created.getRoomId());
        assertThat(found.getOwnerPlayerId()).isEqualTo("player-1");
    }

    @Test
    void getRoomThrowsForUnknownId() {
        assertThatThrownBy(() -> service.getRoom("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("房间不存在");
    }

    @Test
    void joinSeatAssignsPlayerToRequestedSeat() {
        RoomState room = service.createRoom("player-1");
        RoomState updated = service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThat(updated.getSeats()).containsKey(PlayerSeat.NORTH);
        assertThat(updated.getSeats().get(PlayerSeat.NORTH).getPlayerId()).isEqualTo("player-2");
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    void joinSeatRejectsOccupiedSeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.SOUTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已被占用");
    }

    @Test
    void joinSeatRejectsPlayerAlreadySeated() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), "player-1", PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已入座");
    }

    @Test
    void joinSeatRejectsNullPlayerId() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), null, PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinSeatRejectsNullSeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), "player-2", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leaveSeatClearsPlayerSeat() {
        RoomState room = service.createRoom("player-1");
        RoomState updated = service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);

        assertThat(updated.getSeats()).doesNotContainKey(PlayerSeat.SOUTH);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    void leaveSeatRejectsEmptySeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无人占用");
    }

    @Test
    void leaveSeatRejectsOtherPlayersSeat() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThatThrownBy(() -> service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能离开自己");
    }

    @Test
    void multiplePlayersCanJoinDifferentSeats() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.joinSeat(room.getRoomId(), "player-3", PlayerSeat.WEST);
        RoomState updated = service.joinSeat(room.getRoomId(), "player-4", PlayerSeat.EAST);

        assertThat(updated.getSeats()).hasSize(4);
        assertThat(updated.getSeats().get(PlayerSeat.SOUTH).getPlayerId()).isEqualTo("player-1");
        assertThat(updated.getSeats().get(PlayerSeat.NORTH).getPlayerId()).isEqualTo("player-2");
        assertThat(updated.getVersion()).isEqualTo(4);
        assertThat(updated.getSeats().get(PlayerSeat.WEST).getPlayerId()).isEqualTo("player-3");
        assertThat(updated.getSeats().get(PlayerSeat.EAST).getPlayerId()).isEqualTo("player-4");
    }

    @Test
    void startGameCreatesGameAndMovesRoomToPlaying() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        GameState game = service.startGame(room.getRoomId(), "player-1");

        assertThat(game.getId()).isNotBlank();
        assertThat(game.getRoomId()).isEqualTo(room.getRoomId());
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.SOUTH, "player-1");
        assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.NORTH, "player-2");
        assertThat(game.getSeatOwners().get(PlayerSeat.WEST)).isNull();
        assertThat(game.getSeatOwners().get(PlayerSeat.EAST)).isNull();

        RoomState updatedRoom = service.getRoom(room.getRoomId());
        assertThat(updatedRoom.getPhase()).isEqualTo(RoomPhase.PLAYING);
        assertThat(updatedRoom.getGameId()).isEqualTo(game.getId());
        assertThat(updatedRoom.getVersion()).isEqualTo(3);
    }

    @Test
    void startGameRejectsNonOwner() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThatThrownBy(() -> service.startGame(room.getRoomId(), "player-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("房主");
    }

    @Test
    void startGameRejectsNonWaitingRoom() {
        RoomState room = service.createRoom("player-1");
        service.startGame(room.getRoomId(), "player-1");

        assertThatThrownBy(() -> service.startGame(room.getRoomId(), "player-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在等待状态");
    }

    private static class FakeRoomRepository implements RoomRepository {
        final Map<String, RoomState> store = new HashMap<>();

        @Override
        public RoomState save(RoomState room) {
            store.put(room.getRoomId(), room);
            return room;
        }

        @Override
        public Optional<RoomState> find(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    private static class FakeGameRepository implements GameRepository {
        final Map<String, GameState> store = new HashMap<>();

        @Override
        public GameState save(GameState game) {
            store.put(game.getId(), game);
            return game;
        }

        @Override
        public Optional<GameState> find(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }
}
