package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.RoomState;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RoomStateDto(
        String roomId,
        String phase,
        String ownerPlayerId,
        String gameId,
        long version,
        Map<String, SeatInfo> seats
) {
    public record SeatInfo(String playerId, String displayName) {
    }

    public static RoomStateDto from(RoomState room, Function<String, String> displayNameResolver) {
        Map<String, SeatInfo> seatMap = room.getSeats().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> {
                            String playerId = e.getValue().getPlayerId();
                            return new SeatInfo(playerId, displayNameResolver.apply(playerId));
                        }
                ));
        return new RoomStateDto(
                room.getRoomId(),
                room.getPhase().name(),
                room.getOwnerPlayerId(),
                room.getGameId(),
                room.getVersion(),
                seatMap
        );
    }
}
