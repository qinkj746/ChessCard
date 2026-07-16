package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameNotFoundException;
import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("room not found");
    }

    @Test
    void listJoinableRoomsIncludesWaitingRoomsWithOpenSeats() {
        RoomState room = service.createRoom("player-1");

        List<RoomState> rooms = service.listJoinableRooms();

        assertThat(rooms).extracting(RoomState::getRoomId).contains(room.getRoomId());
    }

    @Test
    void listJoinableRoomsExcludesFullAndPlayingRooms() {
        RoomState open = service.createRoom("open-owner");
        RoomState full = service.createRoom("full-owner");
        service.joinSeat(full.getRoomId(), "full-2", PlayerSeat.WEST);
        service.joinSeat(full.getRoomId(), "full-3", PlayerSeat.NORTH);
        service.joinSeat(full.getRoomId(), "full-4", PlayerSeat.EAST);
        RoomState playing = service.createRoom("playing-owner");
        service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.WEST);
        service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.NORTH);
        service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.EAST);
        service.startGame(playing.getRoomId(), "playing-owner");

        List<RoomState> rooms = service.listJoinableRooms();

        assertThat(rooms).extracting(RoomState::getRoomId)
                .contains(open.getRoomId())
                .doesNotContain(full.getRoomId(), playing.getRoomId());
    }

    @Test
    void listJoinableRoomsExcludesWaitingRoomsWithoutOwnerSeat() {
        RoomState stale = RoomState.create("owner");
        stale.getSeats().remove(PlayerSeat.SOUTH);
        roomRepo.save(stale);

        List<RoomState> rooms = service.listJoinableRooms();

        assertThat(rooms).extracting(RoomState::getRoomId)
                .doesNotContain(stale.getRoomId());
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
    void joinSeatIgnoresStalePlayerIdOnBotSeat() {
        RoomState room = service.createRoom("player-1");
        RoomSeat bot = RoomSeat.bot(PlayerSeat.WEST, Instant.now());
        bot.setPlayerId("stale-bot-id");
        room.getSeats().put(PlayerSeat.WEST, bot);

        RoomState updated = service.joinSeat(room.getRoomId(), "stale-bot-id", PlayerSeat.NORTH);

        assertThat(updated.getSeats().get(PlayerSeat.NORTH).getPlayerId()).isEqualTo("stale-bot-id");
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
    void ownerLeavingWaitingRoomRemovesRoom() {
        RoomState room = service.createRoom("player-1");

        service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);

        assertThat(roomRepo.store).doesNotContainKey(room.getRoomId());
        assertThat(service.listJoinableRooms()).extracting(RoomState::getRoomId)
                .doesNotContain(room.getRoomId());
        assertThatThrownBy(() -> service.getRoom(room.getRoomId()))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void ownerLeavingWaitingRoomTransfersOwnershipWhenAnotherHumanRemains() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        RoomState updated = service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);

        assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats()).doesNotContainKey(PlayerSeat.SOUTH);
        assertThat(roomRepo.store).containsKey(room.getRoomId());
    }

    @Test
    void nonOwnerLeavingTransferredWaitingRoomPreservesCurrentOwner() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.WEST);
        service.joinSeat(room.getRoomId(), "player-3", PlayerSeat.NORTH);
        service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);
        service.joinSeat(room.getRoomId(), "player-4", PlayerSeat.SOUTH);

        RoomState updated = service.leaveSeat(room.getRoomId(), "player-3", PlayerSeat.NORTH);

        assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats()).doesNotContainKey(PlayerSeat.NORTH);
        assertThat(updated.getSeats().get(PlayerSeat.WEST).getPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats().get(PlayerSeat.SOUTH).getPlayerId()).isEqualTo("player-4");
        assertThat(roomRepo.store).containsKey(room.getRoomId());
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
    void addBotAssignsBotToRequestedSeat() {
        RoomState room = service.createRoom("player-1");

        RoomState updated = service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThat(updated.getSeats()).containsKey(PlayerSeat.WEST);
        assertThat(updated.getSeats().get(PlayerSeat.WEST).isBot()).isTrue();
        assertThat(updated.getSeats().get(PlayerSeat.WEST).getPlayerId()).isNull();
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    void addBotRejectsNonOwner() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-2", null))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("房主");
    }

    @Test
    void addBotRejectsBlankActorPlayerId() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.addBot(room.getRoomId(), "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("playerId");
    }

    @Test
    void addBotRejectsNullSeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addBotRejectsOccupiedSeat() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已被占用");
    }

    @Test
    void removeBotClearsBotSeat() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        RoomState updated = service.removeBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThat(updated.getSeats()).doesNotContainKey(PlayerSeat.WEST);
        assertThat(updated.getVersion()).isEqualTo(3);
    }

    @Test
    void removeBotRejectsNonOwner() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-2", null))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("房主");
    }

    @Test
    void removeBotRejectsBlankActorPlayerId() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("playerId");
    }

    @Test
    void removeBotRejectsNullSeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeBotRejectsEmptySeat() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("人机");
    }

    @Test
    void removeBotRejectsHumanSeat() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", PlayerSeat.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("人机");
    }

    @Test
    void botManagementRejectsPlayingRoomBeforeSeatValidation() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        service.startGame(room.getRoomId(), "player-1");

        assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在等待状态");
        assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在等待状态");
    }

    @Test
    void humanCannotJoinBotSeat() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已被占用");
    }

    @Test
    void humanCannotLeaveBotSeat() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThatThrownBy(() -> service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("人机");
        assertThat(service.getRoom(room.getRoomId()).getSeats().get(PlayerSeat.WEST).isBot()).isTrue();
    }

    @Test
    void humanCannotLeaveBotSeatWithStalePlayerId() {
        RoomState room = service.createRoom("player-1");
        RoomSeat bot = RoomSeat.bot(PlayerSeat.WEST, Instant.now());
        bot.setPlayerId("stale-bot-id");
        room.getSeats().put(PlayerSeat.WEST, bot);

        assertThatThrownBy(() -> service.leaveSeat(room.getRoomId(), "stale-bot-id", PlayerSeat.WEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("人机");
        assertThat(service.getRoom(room.getRoomId()).getSeats()).containsEntry(PlayerSeat.WEST, bot);
    }

    @Test
    void startGameRejectsIncompleteRoom() {
        RoomState room = service.createRoom("player-1");

        assertThatThrownBy(() -> service.startGame(room.getRoomId(), "player-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4");
    }

    @Test
    void startGameCreatesGameAndMovesRoomToPlaying() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);

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
        assertThat(updatedRoom.getVersion()).isEqualTo(5);
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
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        service.startGame(room.getRoomId(), "player-1");

        assertThatThrownBy(() -> service.startGame(room.getRoomId(), "player-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在等待状态");
    }

    @Test
    void leavingPlayingRoomConvertsSeatToBotAndKeepsRoomWhenHumanRemains() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        GameState game = service.startGame(room.getRoomId(), "player-1");

        RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-2");

        assertThat(updated.getPhase()).isEqualTo(RoomPhase.PLAYING);
        assertThat(updated.getSeats().get(PlayerSeat.NORTH).isBot()).isTrue();
        assertThat(updated.getSeats().get(PlayerSeat.NORTH).getPlayerId()).isNull();
        assertThat(roomRepo.store).containsKey(room.getRoomId());
        assertThat(gameService.getGame(game.getId()).getSeatOwners().get(PlayerSeat.NORTH)).isNull();
    }

    @Test
    void ownerLeavingPlayingRoomTransfersOwnershipWhenHumanRemains() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        service.startGame(room.getRoomId(), "player-1");

        RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-1");

        assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats().get(PlayerSeat.SOUTH).isBot()).isTrue();
    }

    @Test
    void nonOwnerLeavingTransferredPlayingRoomPreservesCurrentOwner() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.WEST);
        service.joinSeat(room.getRoomId(), "player-3", PlayerSeat.NORTH);
        service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);
        service.joinSeat(room.getRoomId(), "player-4", PlayerSeat.SOUTH);
        service.addBot(room.getRoomId(), "player-2", PlayerSeat.EAST);
        GameState game = service.startGame(room.getRoomId(), "player-2");

        RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-3");

        assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats().get(PlayerSeat.NORTH).isBot()).isTrue();
        assertThat(updated.getSeats().get(PlayerSeat.WEST).getPlayerId()).isEqualTo("player-2");
        assertThat(updated.getSeats().get(PlayerSeat.SOUTH).getPlayerId()).isEqualTo("player-4");
        assertThat(gameService.getGame(game.getId()).getSeatOwners().get(PlayerSeat.NORTH)).isNull();
        assertThat(roomRepo.store).containsKey(room.getRoomId());
    }

    @Test
    void leavingPlayingRoomDeletesRoomWhenNoHumanRemains() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        GameState game = service.startGame(room.getRoomId(), "player-1");

        RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-1");

        assertThat(updated.getSeats().get(PlayerSeat.SOUTH).isBot()).isTrue();
        assertThat(roomRepo.store).doesNotContainKey(room.getRoomId());
        assertThat(gameService.getGame(game.getId()).getSeatOwners().get(PlayerSeat.SOUTH)).isNull();
    }

    @Test
    void leavePlayingRoomRejectsPlayerWhoIsNotSeated() {
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        service.startGame(room.getRoomId(), "player-1");

        assertThatThrownBy(() -> service.leavePlayingRoom(room.getRoomId(), "player-2"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("\u672a\u5165\u5ea7");
    }

    @Test
    void departedPlayerCanNoLongerActAsOldRoomGameSeat() {
        RoomState room = service.createRoom("player-1");
        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        GameState game = service.startGame(room.getRoomId(), "player-1");

        service.leavePlayingRoom(room.getRoomId(), "player-2");

        assertThatThrownBy(() -> gameService.play(game.getId(), PlayerSeat.NORTH,
                List.of(gameService.getGame(game.getId()).getHands().get(PlayerSeat.NORTH).get(0)), "player-2"))
                .isInstanceOf(PermissionDeniedException.class);
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

        @Override
        public void delete(String id) {
            store.remove(id);
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
}
