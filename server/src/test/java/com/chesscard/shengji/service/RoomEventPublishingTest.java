package com.chesscard.shengji.service;

import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.websocket.RoomEventType;
import com.chesscard.shengji.api.websocket.WebSocketRoomEventPublisher;
import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomEventPublishingTest {
    @Test
    void joinSeatPublishesRoomUpdatedEvent() {
        RecordingRoomEventPublisher publisher = new RecordingRoomEventPublisher();
        FakeRoomRepository roomRepo = new FakeRoomRepository();
        GameService gameService = new GameService(new FakeGameRepository(), new AiPlayer(), publisher);
        RoomService service = new RoomService(roomRepo, gameService, publisher);
        RoomState room = service.createRoom("player-1");
        publisher.events.clear();

        service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

        assertThat(publisher.events).hasSize(1);
        RoomEvent event = publisher.events.get(0);
        assertThat(event.type()).isEqualTo(RoomEventType.ROOM_UPDATED);
        assertThat(event.roomId()).isEqualTo(room.getRoomId());
        assertThat(event.gameId()).isNull();
        assertThat(event.version()).isEqualTo(2L);
        assertThat(event.payload()).isNull();
    }

    @Test
    void addAndRemoveBotPublishRoomUpdatedEvents() {
        RecordingRoomEventPublisher publisher = new RecordingRoomEventPublisher();
        FakeRoomRepository roomRepo = new FakeRoomRepository();
        GameService gameService = new GameService(new FakeGameRepository(), new AiPlayer(), publisher);
        RoomService service = new RoomService(roomRepo, gameService, publisher);
        RoomState room = service.createRoom("player-1");
        publisher.events.clear();

        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.removeBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

        assertThat(publisher.events)
                .extracting(RoomEvent::type)
                .containsExactly(RoomEventType.ROOM_UPDATED, RoomEventType.ROOM_UPDATED);
        assertThat(publisher.events)
                .extracting(RoomEvent::version)
                .containsExactly(2L, 3L);
    }

    @Test
    void startGamePublishesRoomAndGameUpdatedEvents() {
        RecordingRoomEventPublisher publisher = new RecordingRoomEventPublisher();
        FakeRoomRepository roomRepo = new FakeRoomRepository();
        GameService gameService = new GameService(new FakeGameRepository(), new AiPlayer(), publisher);
        RoomService service = new RoomService(roomRepo, gameService, publisher);
        RoomState room = service.createRoom("player-1");
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
        service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
        publisher.events.clear();

        GameState game = service.startGame(room.getRoomId(), "player-1");

        assertThat(publisher.events)
                .extracting(RoomEvent::type)
                .containsExactly(RoomEventType.GAME_UPDATED, RoomEventType.ROOM_UPDATED);
        assertThat(publisher.events)
                .allSatisfy(event -> assertThat(event.roomId()).isEqualTo(room.getRoomId()));
        assertThat(publisher.events.get(0).gameId()).isEqualTo(game.getId());
        assertThat(publisher.events.get(1).gameId()).isEqualTo(game.getId());
        assertThat(publisher.events.get(1).version()).isEqualTo(5L);
    }

    @Test
    void declaringInRoomPublishesGameUpdatedEvent() {
        RecordingRoomEventPublisher publisher = new RecordingRoomEventPublisher();
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer(), publisher);
        GameState game = declareGameWithRoom(repository);
        publisher.events.clear();

        service.declare(game.getId(), Suit.SPADE, "player-1");

        assertThat(publisher.events).hasSize(1);
        RoomEvent event = publisher.events.get(0);
        assertThat(event.type()).isEqualTo(RoomEventType.GAME_UPDATED);
        assertThat(event.roomId()).isEqualTo("room-1");
        assertThat(event.gameId()).isEqualTo(game.getId());
    }

    @Test
    void roomEventTypeDefinesFutureSeatEvents() {
        assertThat(RoomEventType.valueOf("PLAYER_JOINED")).isEqualTo(RoomEventType.PLAYER_JOINED);
        assertThat(RoomEventType.valueOf("PLAYER_LEFT")).isEqualTo(RoomEventType.PLAYER_LEFT);
    }
    @Test
    void webSocketPublisherSendsRoomEventsToRoomTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketRoomEventPublisher publisher = new WebSocketRoomEventPublisher(messagingTemplate);
        RoomEvent event = new RoomEvent(RoomEventType.ROOM_UPDATED, "room-1", null);

        publisher.publish(event);

        verify(messagingTemplate).convertAndSend("/topic/rooms/room-1", event);
    }

    private static class RecordingRoomEventPublisher implements RoomEventPublisher {
        final List<RoomEvent> events = new ArrayList<>();

        @Override
        public void publish(RoomEvent event) {
            events.add(event);
        }
    }

    private static GameState declareGameWithRoom(FakeGameRepository repository) {
        GameState game = new GameState();
        game.setRoomId("room-1");
        game.setPhase(GamePhase.DECLARE);
        game.setLevelRank(Rank.TWO);
        game.setCurrentTurn(PlayerSeat.SOUTH);
        game.getSeatOwners().put(PlayerSeat.SOUTH, "player-1");
        game.getSeatOwners().put(PlayerSeat.WEST, null);
        game.getSeatOwners().put(PlayerSeat.NORTH, null);
        game.getSeatOwners().put(PlayerSeat.EAST, null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                new Card(Suit.SPADE, Rank.TWO, 0),
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(Suit.SPADE, Rank.THREE, 0),
                new Card(Suit.HEART, Rank.FOUR, 0)
        )));
        game.getKitty().addAll(List.of(
                new Card(Suit.CLUB, Rank.THREE, 0),
                new Card(Suit.CLUB, Rank.THREE, 1),
                new Card(Suit.CLUB, Rank.FOUR, 0),
                new Card(Suit.CLUB, Rank.FOUR, 1),
                new Card(Suit.DIAMOND, Rank.THREE, 0),
                new Card(Suit.DIAMOND, Rank.THREE, 1),
                new Card(Suit.DIAMOND, Rank.FOUR, 0),
                new Card(Suit.DIAMOND, Rank.FOUR, 1)
        ));
        repository.save(game);
        return game;
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
