package com.chesscard.shengji.service;

import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.websocket.RoomEventType;
import com.chesscard.shengji.domain.ChatMessage;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatServiceTest {
    private final FakeChatMessageRepository messages = new FakeChatMessageRepository();
    private final FakeRoomRepository rooms = new FakeRoomRepository();
    private final CapturingPublisher publisher = new CapturingPublisher();
    private final ChatService service = new ChatService(messages, rooms, publisher);

    @Test
    void sendMessageStoresMessageAndPublishesRoomEvent() {
        RoomState room = roomWithSouthPlayer("room-1", "player-1");
        rooms.save(room);

        ChatMessage message = service.sendMessage("room-1", "player-1", " hello ");

        assertThat(message.getMessageId()).isNotBlank();
        assertThat(message.getRoomId()).isEqualTo("room-1");
        assertThat(message.getSenderPlayerId()).isEqualTo("player-1");
        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(message.getSentAt()).isNotNull();
        assertThat(messages.saved).containsExactly(message);
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).type()).isEqualTo(RoomEventType.CHAT_MESSAGE);
        assertThat(publisher.events.get(0).roomId()).isEqualTo("room-1");
        assertThat(publisher.events.get(0).payload()).isEqualTo(message);
    }

    @Test
    void sendMessageRejectsBlankContent() {
        rooms.save(roomWithSouthPlayer("room-1", "player-1"));

        assertThatThrownBy(() -> service.sendMessage("room-1", "player-1", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");

        assertThat(messages.saved).isEmpty();
    }

    @Test
    void sendMessageRejectsNonMember() {
        rooms.save(roomWithSouthPlayer("room-1", "player-1"));

        assertThatThrownBy(() -> service.sendMessage("room-1", "player-2", "hello"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void listMessagesReturnsRoomMessagesInSentOrder() {
        rooms.save(roomWithSouthPlayer("room-1", "player-1"));
        ChatMessage second = new ChatMessage("m2", "room-1", "player-1", "second", Instant.parse("2026-07-10T08:02:00Z"));
        ChatMessage first = new ChatMessage("m1", "room-1", "player-1", "first", Instant.parse("2026-07-10T08:01:00Z"));
        ChatMessage otherRoom = new ChatMessage("m3", "room-2", "player-1", "other", Instant.parse("2026-07-10T08:00:00Z"));
        messages.save(second);
        messages.save(first);
        messages.save(otherRoom);

        List<ChatMessage> result = service.listMessages("room-1");

        assertThat(result).containsExactly(first, second);
    }

    private static RoomState roomWithSouthPlayer(String roomId, String playerId) {
        RoomState room = new RoomState();
        room.setRoomId(roomId);
        room.getSeats().put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, playerId, Instant.now()));
        return room;
    }

    private static class FakeChatMessageRepository implements ChatMessageRepository {
        final List<ChatMessage> saved = new ArrayList<>();

        @Override
        public ChatMessage save(ChatMessage message) {
            saved.add(message);
            return message;
        }

        @Override
        public List<ChatMessage> findByRoomId(String roomId) {
            return saved.stream()
                    .filter(message -> message.getRoomId().equals(roomId))
                    .sorted(Comparator.comparing(ChatMessage::getSentAt))
                    .toList();
        }
    }

    private static class FakeRoomRepository implements RoomRepository {
        final Map<String, RoomState> rooms = new HashMap<>();

        @Override
        public RoomState save(RoomState room) {
            rooms.put(room.getRoomId(), room);
            return room;
        }

        @Override
        public Optional<RoomState> find(String id) {
            return Optional.ofNullable(rooms.get(id));
        }
    }

    private static class CapturingPublisher implements RoomEventPublisher {
        final List<RoomEvent> events = new ArrayList<>();

        @Override
        public void publish(RoomEvent event) {
            events.add(event);
        }
    }
}