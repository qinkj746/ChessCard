package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerServiceTest {
    private final FakePlayerRepository repo = new FakePlayerRepository();
    private final PlayerService service = new PlayerService(repo);

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
    }
}
