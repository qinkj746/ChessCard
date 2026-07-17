package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
public class PlayerService {
    private static final Duration ONLINE_WINDOW = Duration.ofSeconds(90);

    private final PlayerRepository repository;
    private final Clock clock;

    @Autowired
    public PlayerService(PlayerRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public PlayerService(PlayerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PlayerProfile createGuest() {
        PlayerProfile profile = PlayerProfile.createGuest();
        return repository.save(profile);
    }

    public PlayerProfile getPlayer(String playerId) {
        return repository.find(playerId)
                .orElseThrow(() -> new IllegalArgumentException("player not found: " + playerId));
    }

    public PlayerProfile markOnline(String playerId) {
        PlayerProfile profile = getPlayer(playerId);
        profile.markSeen(clock.instant());
        return repository.save(profile);
    }

    public List<PlayerProfile> listOnlinePlayers() {
        return repository.findSeenSince(clock.instant().minus(ONLINE_WINDOW));
    }
}
