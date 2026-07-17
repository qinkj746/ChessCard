package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.AuthSession;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.domain.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    private final FakeUserAccountRepository accountRepository = new FakeUserAccountRepository();
    private final FakePlayerRepository playerRepository = new FakePlayerRepository();
    private final AuthService service = new AuthService(accountRepository, playerRepository);

    @Test
    void registerCreatesAccountWithHashedPasswordAndSessionToken() {
        AuthSession session = service.register("alice", "secret123", null);

        assertThat(session.playerId()).isNotBlank();
        assertThat(session.username()).isEqualTo("alice");
        assertThat(session.displayName()).isEqualTo("alice");
        assertThat(session.sessionToken()).isNotBlank();
        UserAccount account = accountRepository.byUsername.get("alice");
        assertThat(account.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(account.getPasswordHash()).startsWith("pbkdf2$");
        assertThat(account.getSessionToken()).isEqualTo(session.sessionToken());
        assertThat(playerRepository.store.get(session.playerId()).isGuest()).isFalse();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        service.register("alice", "secret123", null);

        assertThatThrownBy(() -> service.register("alice", "another123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    void registerRejectsShortPassword() {
        assertThatThrownBy(() -> service.register("alice", "123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void loginRejectsWrongPassword() {
        service.register("alice", "secret123", null);

        assertThatThrownBy(() -> service.login("alice", "bad-password"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void loginReturnsNewSessionToken() {
        AuthSession first = service.register("alice", "secret123", null);

        AuthSession second = service.login("alice", "secret123");

        assertThat(second.playerId()).isEqualTo(first.playerId());
        assertThat(second.username()).isEqualTo("alice");
        assertThat(second.sessionToken()).isNotBlank();
        assertThat(accountRepository.byUsername.get("alice").getSessionToken()).isEqualTo(second.sessionToken());
    }

    @Test
    void registerBindsExistingGuestPlayerId() {
        PlayerProfile guest = PlayerProfile.createGuest();
        playerRepository.save(guest);

        AuthSession session = service.register("alice", "secret123", guest.getPlayerId());

        assertThat(session.playerId()).isEqualTo(guest.getPlayerId());
        assertThat(playerRepository.store.get(guest.getPlayerId()).isGuest()).isFalse();
    }

    @Test
    void logoutClearsSessionToken() {
        AuthSession session = service.register("alice", "secret123", null);

        service.logout(session.sessionToken());

        assertThat(accountRepository.byUsername.get("alice").getSessionToken()).isNull();
    }

    private static class FakeUserAccountRepository implements UserAccountRepository {
        final Map<String, UserAccount> byUsername = new HashMap<>();
        final Map<String, UserAccount> bySessionToken = new HashMap<>();

        @Override
        public UserAccount save(UserAccount account) {
            byUsername.put(account.getUsername(), account);
            bySessionToken.values().removeIf(existing -> existing.getUsername().equals(account.getUsername()));
            if (account.getSessionToken() != null) {
                bySessionToken.put(account.getSessionToken(), account);
            }
            return account;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public Optional<UserAccount> findBySessionToken(String sessionToken) {
            return Optional.ofNullable(bySessionToken.get(sessionToken));
        }
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
