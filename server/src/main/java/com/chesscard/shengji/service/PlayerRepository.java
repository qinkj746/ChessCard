package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    PlayerProfile save(PlayerProfile profile);

    Optional<PlayerProfile> find(String id);

    List<PlayerProfile> findSeenSince(Instant cutoff);
}
