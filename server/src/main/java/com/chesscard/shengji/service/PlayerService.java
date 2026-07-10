package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {
    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public PlayerProfile createGuest() {
        PlayerProfile profile = PlayerProfile.createGuest();
        return repository.save(profile);
    }

    public PlayerProfile getPlayer(String playerId) {
        return repository.find(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家不存在: " + playerId));
    }
}
