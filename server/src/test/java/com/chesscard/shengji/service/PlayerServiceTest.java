package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.HashMap;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerServiceTest {
    private final FakePlayerRepository repo = new FakePlayerRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-10T08:00:00Z"), ZoneOffset.UTC);
    private final PlayerService service = new PlayerService(repo, clock);

    @Test
    void createsGuestPlayerWithStableIdAndDisplayName() {
        PlayerProfile guest = service.createGuest();

        assertThat(guest.getPlayerId()).isNotBlank();
        assertThat(guest.getDisplayName()).startsWith("Guest-");
        assertThat(guest.getDisplayName()).hasSize(11);
        assertThat(guest.isGuest()).isTrue();
        assertThat(guest.getSessionToken()).isNotBlank();
        assertThat(guest.getCreatedAt()).isNotNull();
    }

    @Test
    void eachGuestGetsUniqueId() {
        PlayerProfile first = service.createGuest();
        PlayerProfile second = service.createGuest();

        assertThat(first.getPlayerId()).isNotEqualTo(second.getPlayerId());
    }

    @Test
    void guestsArePersisted() {
        PlayerProfile guest = service.createGuest();

        assertThat(repo.store).containsKey(guest.getPlayerId());
        assertThat(repo.store.get(guest.getPlayerId()).getDisplayName()).isEqualTo(guest.getDisplayName());
    }

    @Test
    void getPlayerReturnsExistingPlayer() {
        PlayerProfile guest = service.createGuest();
        PlayerProfile found = service.getPlayer(guest.getPlayerId());

        assertThat(found.getPlayerId()).isEqualTo(guest.getPlayerId());
        assertThat(found.getDisplayName()).isEqualTo(guest.getDisplayName());
    }

    @Test
    void getPlayerThrowsForUnknownId() {
        assertThatThrownBy(() -> service.getPlayer("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    void heartbeatMarksExistingPlayerOnline() {
        PlayerProfile guest = service.createGuest();

        PlayerProfile online = service.markOnline(guest.getPlayerId());

        assertThat(online.getLastSeenAt()).isEqualTo(Instant.parse("2026-07-10T08:00:00Z"));
        assertThat(repo.store.get(guest.getPlayerId()).getLastSeenAt()).isEqualTo(online.getLastSeenAt());
    }

    @Test
    void listOnlinePlayersReturnsPlayersSeenInTheLastNinetySeconds() {
        PlayerProfile active = new PlayerProfile(
                "active-player",
                "Active",
                false,
                "active-token",
                Instant.parse("2026-07-10T07:00:00Z"),
                Instant.parse("2026-07-10T07:58:31Z")
        );
        PlayerProfile stale = new PlayerProfile(
                "stale-player",
                "Stale",
                false,
                "stale-token",
                Instant.parse("2026-07-10T07:00:00Z"),
                Instant.parse("2026-07-10T07:58:29Z")
        );
        repo.save(active);
        repo.save(stale);

        assertThat(service.listOnlinePlayers())
                .extracting(PlayerProfile::getPlayerId)
                .containsExactly("active-player");
    }

    private static class FakePlayerRepository implements PlayerRepository {
        final Map<String, PlayerProfile> store = new HashMap<>();

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
