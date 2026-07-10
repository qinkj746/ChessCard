package com.chesscard.shengji.api.websocket;

import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.RoomState;

import java.time.Instant;

public record RoomEvent(
        RoomEventType type,
        String roomId,
        String gameId,
        Long version,
        Object payload,
        Instant occurredAt
) {
    public RoomEvent(RoomEventType type, String roomId, String gameId) {
        this(type, roomId, gameId, null, null, Instant.now());
    }

    public RoomEvent(RoomEventType type, String roomId, String gameId, Long version, Object payload) {
        this(type, roomId, gameId, version, payload, Instant.now());
    }

    public static RoomEvent roomUpdated(RoomState room) {
        return new RoomEvent(RoomEventType.ROOM_UPDATED, room.getRoomId(), room.getGameId(), room.getVersion(), null);
    }

    public static RoomEvent gameUpdated(GameState game) {
        return new RoomEvent(RoomEventType.GAME_UPDATED, game.getRoomId(), game.getId());
    }
}