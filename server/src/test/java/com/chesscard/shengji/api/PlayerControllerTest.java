package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.PlayerProfileDto;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.service.PlayerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerControllerTest {
    @Test
    void createGuestReturnsProfileDto() {
        PlayerController controller = new PlayerController(new StubPlayerService());

        PlayerProfileDto dto = controller.createGuest();

        assertThat(dto.playerId()).isNotBlank();
        assertThat(dto.displayName()).startsWith("Guest-");
        assertThat(dto.guest()).isTrue();
        assertThat(dto.sessionToken()).isNotBlank();
    }

    @Test
    void refreshReturnsExistingPlayer() {
        StubPlayerService stub = new StubPlayerService();
        PlayerController controller = new PlayerController(stub);
        PlayerProfileDto created = controller.createGuest();

        PlayerProfileDto refreshed = controller.refresh(created.playerId());

        assertThat(refreshed.playerId()).isEqualTo(created.playerId());
        assertThat(refreshed.displayName()).isEqualTo(created.displayName());
    }

    @Test
    void refreshThrowsForUnknownPlayer() {
        PlayerController controller = new PlayerController(new StubPlayerService());

        assertThatThrownBy(() -> controller.refresh("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class StubPlayerService extends PlayerService {
        StubPlayerService() {
            super(new FakePlayerRepository());
        }
    }

    private static class FakePlayerRepository implements com.chesscard.shengji.service.PlayerRepository {
        private final java.util.Map<String, PlayerProfile> store = new java.util.HashMap<>();

        @Override
        public PlayerProfile save(PlayerProfile profile) {
            store.put(profile.getPlayerId(), profile);
            return profile;
        }

        @Override
        public java.util.Optional<PlayerProfile> find(String id) {
            return java.util.Optional.ofNullable(store.get(id));
        }
    }
}
