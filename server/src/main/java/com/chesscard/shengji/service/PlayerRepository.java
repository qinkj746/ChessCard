package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;

import java.util.Optional;

public interface PlayerRepository {
    PlayerProfile save(PlayerProfile profile);

    Optional<PlayerProfile> find(String id);
}
