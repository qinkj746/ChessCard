package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.RoomState;

import java.util.Optional;

public interface RoomRepository {
    RoomState save(RoomState room);

    Optional<RoomState> find(String id);
}
