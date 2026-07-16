package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CreateRoomRequest;
import com.chesscard.shengji.api.dto.GameStateDto;
import com.chesscard.shengji.api.dto.JoinSeatRequest;
import com.chesscard.shengji.api.dto.RoomStateDto;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.AiPlayer;
import com.chesscard.shengji.service.GameRepository;
import com.chesscard.shengji.service.GameService;
import com.chesscard.shengji.service.PlayerRepository;
import com.chesscard.shengji.service.PlayerService;
import com.chesscard.shengji.service.RoomRepository;
import com.chesscard.shengji.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        assertThat(dto.seats().get("SOUTH").isBot()).isFalse();
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
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void missingRoomMapsToNotFoundOverHttp() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/rooms/missing-room"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }
    @Test
    void listReturnsJoinableRoomDtosOverHttp() throws Exception {
        playerRepo.save(new PlayerProfile("player-1", "Alice", false, "token-1", Instant.now()));
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(created.roomId()))
                .andExpect(jsonPath("$[0].phase").value("WAITING"))
                .andExpect(jsonPath("$[0].seats.SOUTH.displayName").value("Alice"));
    }

    @Test
    void joinSeatReturnsUpdatedRoom() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        RoomStateDto updated = controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));

        assertThat(updated.seats()).containsKey("NORTH");
        assertThat(updated.seats().get("NORTH").playerId()).isEqualTo("player-2");
        assertThat(updated.seats().get("NORTH").isBot()).isFalse();
    }

    @Test
    void ownerCanAddAndRemoveBotSeat() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

        RoomStateDto withBot = controller.addBot(created.roomId(), "west", new JoinSeatRequest("player-1"));

        assertThat(withBot.seats().get("WEST").playerId()).isNull();
        assertThat(withBot.seats().get("WEST").displayName()).isEqualTo("人机");
        assertThat(withBot.seats().get("WEST").isBot()).isTrue();

        RoomStateDto withoutBot = controller.removeBot(created.roomId(), "west", new JoinSeatRequest("player-1"));

        assertThat(withoutBot.seats()).doesNotContainKey("WEST");
    }

    @Test
    void nonOwnerCannotAddBotSeat() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));

        assertThatThrownBy(() -> controller.addBot(created.roomId(), "west", new JoinSeatRequest("player-2")))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void addBotHttpRequestReturnsForbiddenForNonOwner() throws Exception {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        controller.joinSeat(created.roomId(), "north", new JoinSeatRequest("player-2"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/api/rooms/{id}/seats/west/bot", created.roomId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"player-2\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void ownerCanAddAndRemoveBotSeatOverHttp() throws Exception {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = "/api/rooms/{id}/seats/west/bot";
        String ownerRequest = "{\"playerId\":\"player-1\"}";

        mockMvc.perform(post(path, created.roomId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats.WEST.isBot").value(true))
                .andExpect(jsonPath("$.seats.WEST.displayName").value("人机"))
                .andExpect(jsonPath("$.seats.WEST.playerId").value(nullValue()));

        mockMvc.perform(delete(path, created.roomId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats.WEST").doesNotExist());
    }

    @Test
    void botRoutesRejectMissingPlayerRequest() {
        RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

        assertThatThrownBy(() -> controller.addBot(created.roomId(), "west", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.removeBot(created.roomId(), "west", new JoinSeatRequest(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roomDtoDoesNotResolveDisplayNameForBotSeat() {
        RoomState room = RoomState.create("player-1");
        room.getSeats().put(PlayerSeat.WEST, RoomSeat.bot(PlayerSeat.WEST, Instant.now()));
        List<String> resolvedPlayerIds = new ArrayList<>();

        RoomStateDto dto = RoomStateDto.from(room, playerId -> {
            resolvedPlayerIds.add(playerId);
            return "resolved-" + playerId;
        });

        assertThat(resolvedPlayerIds).containsExactly("player-1");
        assertThat(dto.seats().get("WEST").displayName()).isEqualTo("人机");
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
        service.addBot(created.roomId(), "player-1", PlayerSeat.WEST);
        service.addBot(created.roomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(created.roomId(), "player-1", PlayerSeat.EAST);

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

    @Test
    void leavePlayingRoomConvertsExitedPlayerToBot() throws Exception {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        service.startGame(room.getRoomId(), "player-1");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(delete("/api/rooms/{id}/players", room.getRoomId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"player-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
                .andExpect(jsonPath("$.phase").value("PLAYING"))
                .andExpect(jsonPath("$.seats.NORTH.isBot").value(true))
                .andExpect(jsonPath("$.seats.NORTH.playerId").value(nullValue()));
    }

    private static class FakeRoomRepository implements RoomRepository {
        final Map<String, RoomState> store = new HashMap<>();

        @Override
        public RoomState save(RoomState room) {
            store.put(room.getRoomId(), room);
            return room;
        }

        @Override
        public void delete(String id) {
            store.remove(id);
        }

        @Override
        public Optional<RoomState> find(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<RoomState> findJoinableWaitingRooms() {
            return store.values().stream()
                    .filter(room -> room.getPhase() == RoomPhase.WAITING)
                    .filter(room -> room.getSeats().size() < 4)
                    .toList();
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

        @Override
        public Optional<GameState> findByRoomId(String roomId) {
            return store.values().stream()
                    .filter(game -> roomId.equals(game.getRoomId()))
                    .findFirst();
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
