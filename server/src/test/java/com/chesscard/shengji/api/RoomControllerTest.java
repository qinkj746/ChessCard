package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CreateRoomRequest;
import com.chesscard.shengji.api.dto.GameStateDto;
import com.chesscard.shengji.api.dto.JoinSeatRequest;
import com.chesscard.shengji.api.dto.RoomStateDto;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.AiPlayer;
import com.chesscard.shengji.service.GameRepository;
import com.chesscard.shengji.service.GameService;
import com.chesscard.shengji.service.PlayerRepository;
import com.chesscard.shengji.service.PlayerService;
import com.chesscard.shengji.service.RoomRepository;
import com.chesscard.shengji.service.RoomService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomControllerTest {
    private final FakeRoomRepository roomRepo = new FakeRoomRepository();
    private final FakeGameRepository gameRepo = new FakeGameRepository();
    private final GameService gameService = new GameService(gameRepo, new AiPlayer());
    private final RoomService service = new RoomService(roomRepo, gameService);
    private final FakePlayerRepository playerRepo = new FakePlayerRepository();
    private final PlayerService playerService = new PlayerService(playerRepo);
    private final RoomController controller = new RoomController(service, playerService);

    @Test
    void createReturnsRoomDtoWithOwnerOnSouth() {
        RoomStateDto dto = controller.create(new CreateRoomRequest("player-1"));

        assertThat(dto.roomId()).isNotBlank();
        assertThat(dto.phase()).isEqualTo("WAITING");
        assertThat(dto.ownerPlayerId()).isEqualTo("player-1");
        assertThat(dto.seats()).containsKey("SOUTH");
        assertThat(dto.seats().get("SOUTH").playerId()).isEqualTo("player-1");
    }

    @Test
    void createReturnsSeatDisplayNameFromPlayerProfile() {
        playerRepo.save(new PlayerProfile("player-1", "Alice", false, "token-1", Instant.now()));

        RoomStateDto dto = controller.create(new CreateRoomRequest("player-1"));

        assertThat(dto.seats().get("SOUTH").playerId()).isEqualTo("player-1");
        assertThat(dto.seats().get("SOUTH").displayName()).isEqualTo("Alice");
    }

    @Test
    void createFallsBackToPlayerIdWhenProfileIsMissing() {
        RoomStateDto dto = controller.create(new CreateRoomRequest("stale-player"));

        assertThat(dto.seats().get("SOUTH").displayName()).isEqualTo("stale-player");
    }

    @Test
    void createRejectsBlankPlayerId() {
        assertThatThrownBy(() -> controller.create(new CreateRoomRequest("")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.create(new CreateRoomRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getReturnsExistingRoom() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        RoomStateDto found = controller.get(created.roomId());

        assertThat(found.roomId()).isEqualTo(created.roomId());
        assertThat(found.ownerPlayerId()).isEqualTo("player-1");
    }

    @Test
    void getThrowsForUnknownRoom() {
        assertThatThrownBy(() -> controller.get("unknown-room"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinSeatReturnsUpdatedRoom() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        RoomStateDto updated = controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));

        assertThat(updated.seats()).containsKey("NORTH");
        assertThat(updated.seats().get("NORTH").playerId()).isEqualTo("player-2");
    }

    @Test
    void joinSeatRejectsOccupiedSeat() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

        assertThatThrownBy(() -> controller.joinSeat(created.roomId(), "south", new JoinSeatRequest("player-2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinSeatRejectsInvalidSeat() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

        assertThatThrownBy(() -> controller.joinSeat(created.roomId(), "invalid", new JoinSeatRequest("player-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效座位");
    }

    @Test
    void leaveSeatReturnsUpdatedRoom() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        RoomStateDto updated = controller.leaveSeat(created.roomId(), "south", new JoinSeatRequest("player-1"));

        assertThat(updated.seats()).doesNotContainKey("SOUTH");
    }

    @Test
    void leaveSeatRejectsOtherPlayersSeat() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));

        assertThatThrownBy(() -> controller.leaveSeat(created.roomId(), "north", new JoinSeatRequest("player-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startReturnsGameState() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

        GameStateDto game = controller.start(created.roomId(), new JoinSeatRequest("player-1"));

        assertThat(game.id()).isNotBlank();
        assertThat(game.phase()).isEqualTo(GamePhase.DECLARE);
    }

    @Test
    void startRejectsNonOwner() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));

        assertThatThrownBy(() -> controller.start(created.roomId(), new JoinSeatRequest("player-2")))
                .isInstanceOf(IllegalArgumentException.class);
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

    private static class FakePlayerRepository implements PlayerRepository {
        final Map<String, PlayerProfile> store = new HashMap<>();

        @Override
        public PlayerProfile save(PlayerProfile profile) {
            store.put(profile.getPlayerId(), profile);
            return profile;
        }

        @Override
        public Optional<PlayerProfile> find(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }
}
