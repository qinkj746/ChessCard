package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.RoomState;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    RoomState save(RoomState room);

    void delete(String id);

    Optional<RoomState> find(String id);

    default List<RoomState> findJoinableWaitingRooms() {
        return List.of();
    }
}
