package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.HeartbeatRequest;
import com.chesscard.shengji.api.dto.OnlinePlayerDto;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.service.PlayerRepository;
import com.chesscard.shengji.service.PlayerService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceControllerTest {
    @Test
    void heartbeatMarksPlayerOnlineAndReturnsTheOnlineProfile() {
        FakePlayerRepository repository = new FakePlayerRepository();
        PlayerService service = new PlayerService(
                repository,
                Clock.fixed(Instant.parse("2026-07-10T08:00:00Z"), ZoneOffset.UTC)
        );
        PlayerProfile player = repository.save(new PlayerProfile(
                "player-1",
                "Alice",
                false,
                "token-1",
                Instant.parse("2026-07-10T07:00:00Z")
        ));
        PresenceController controller = new PresenceController(service);

        OnlinePlayerDto result = controller.heartbeat(new HeartbeatRequest(player.getPlayerId()));

        assertThat(result.playerId()).isEqualTo("player-1");
        assertThat(result.displayName()).isEqualTo("Alice");
        assertThat(result.guest()).isFalse();
        assertThat(result.lastSeenAt()).isEqualTo(Instant.parse("2026-07-10T08:00:00Z"));
    }

    private static class FakePlayerRepository implements PlayerRepository {
        private final Map<String, PlayerProfile> store = new HashMap<>();

        @Override
        public PlayerProfile save(PlayerProfile profile) {
            store.put(profile.getPlayerId(), profile);
            return profile;
        }

        @Override
        public Optional<PlayerProfile> find(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<PlayerProfile> findSeenSince(Instant cutoff) {
            return store.values().stream()
                    .filter(profile -> profile.getLastSeenAt() != null)
                    .filter(profile -> !profile.getLastSeenAt().isBefore(cutoff))
                    .toList();
        }
    }
}
