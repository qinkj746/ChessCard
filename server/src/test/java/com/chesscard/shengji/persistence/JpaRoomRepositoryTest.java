package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaRoomRepositoryTest.Config.class)
class JpaRoomRepositoryTest {
    @Autowired
    private RoomRepository repository;

    @Test
    void findJoinableWaitingRoomsReturnsOnlyWaitingRoomsWithOpenSeats() {
        RoomState open = repository.save(RoomState.create("open-owner"));
        RoomState full = repository.save(RoomState.create("full-owner"));
        full.getSeats().put(PlayerSeat.WEST,
                new RoomSeat(PlayerSeat.WEST, "full-2", Instant.now()));
        full.getSeats().put(PlayerSeat.NORTH,
                new RoomSeat(PlayerSeat.NORTH, "full-3", Instant.now()));
        full.getSeats().put(PlayerSeat.EAST,
                new RoomSeat(PlayerSeat.EAST, "full-4", Instant.now()));
        repository.save(full);
        RoomState playing = RoomState.create("playing-owner");
        playing.setPhase(RoomPhase.PLAYING);
        repository.save(playing);

        List<RoomState> rooms = repository.findJoinableWaitingRooms();

        assertThat(rooms).extracting(RoomState::getRoomId)
                .contains(open.getRoomId())
                .doesNotContain(full.getRoomId(), playing.getRoomId());
    }

    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        RoomRepository roomRepository(RoomStateJpaRepository jpaRepository, ObjectMapper objectMapper) {
            return new JpaRoomRepository(jpaRepository, objectMapper);
        }
    }
}
