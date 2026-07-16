package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GameState;

import java.util.Optional;

public interface GameRepository {
    GameState save(GameState game);

    Optional<GameState> find(String id);

    Optional<GameState> findByRoomId(String roomId);
}
