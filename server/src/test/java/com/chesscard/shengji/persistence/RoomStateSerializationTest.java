package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RoomStateSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void roundTripsHumanAndBotSeats() throws Exception {
        RoomState room = RoomState.create("owner");
        room.getSeats().put(
                PlayerSeat.WEST,
                RoomSeat.bot(PlayerSeat.WEST, Instant.parse("2026-07-14T08:00:00Z"))
        );

        String json = objectMapper.writeValueAsString(room);
        RoomState restored = objectMapper.readValue(json, RoomState.class);

        RoomSeat south = restored.getSeats().get(PlayerSeat.SOUTH);
        assertThat(south.isBot()).isFalse();
        assertThat(south.getPlayerId()).isEqualTo("owner");

        RoomSeat west = restored.getSeats().get(PlayerSeat.WEST);
        assertThat(west.isBot()).isTrue();
        assertThat(west.getPlayerId()).isNull();
    }

    @Test
    void treatsLegacySeatWithoutBotMarkerAsHuman() throws Exception {
        String json = """
                {
                  "seat": "SOUTH",
                  "playerId": "legacy-owner",
                  "joinedAt": "2026-07-14T08:00:00Z"
                }
                """;

        RoomSeat seat = objectMapper.readValue(json, RoomSeat.class);

        assertThat(seat.isBot()).isFalse();
        assertThat(seat.getPlayerId()).isEqualTo("legacy-owner");
    }
}
