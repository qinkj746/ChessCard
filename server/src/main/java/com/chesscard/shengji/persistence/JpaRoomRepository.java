package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.RoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaRoomRepository implements RoomRepository {
    private final RoomStateJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public JpaRoomRepository(RoomStateJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public RoomState save(RoomState room) {
        RoomStateEntity entity = new RoomStateEntity();
        entity.setRoomId(room.getRoomId());
        entity.setPhase(room.getPhase().name());
        entity.setOwnerPlayerId(room.getOwnerPlayerId());
        entity.setGameId(room.getGameId());
        entity.setSnapshotJson(toJson(room));
        entity.setUpdatedAt(Instant.now());
        jpaRepository.save(entity);
        return room;
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<RoomState> find(String id) {
        return jpaRepository.findById(id).map(entity -> fromJson(entity.getSnapshotJson()));
    }

    @Override
    public List<RoomState> findJoinableWaitingRooms() {
        return jpaRepository.findByPhaseOrderByUpdatedAtDesc(RoomPhase.WAITING.name()).stream()
                .map(entity -> fromJson(entity.getSnapshotJson()))
                .filter(room -> room.getSeats().size() < 4)
                .toList();
    }

    private String toJson(RoomState room) {
        try {
            return objectMapper.writeValueAsString(room);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("房间快照序列化失败", e);
        }
    }

    private RoomState fromJson(String json) {
        try {
            return objectMapper.readValue(json, RoomState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("房间快照反序列化失败", e);
        }
    }
}
