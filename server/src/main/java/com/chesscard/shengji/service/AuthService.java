package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.AuthSession;
import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.domain.UserAccount;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final UserAccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository accountRepository, PlayerRepository playerRepository) {
        this.accountRepository = accountRepository;
        this.playerRepository = playerRepository;
    }

    public AuthSession register(String username, String password, String playerId) {
        String normalizedUsername = validateUsername(username);
        validatePassword(password);
        if (accountRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }

        String sessionToken = newSessionToken();
        PlayerProfile profile = resolveRegistrationProfile(normalizedUsername, playerId, sessionToken);
        playerRepository.save(profile);

        UserAccount account = new UserAccount(
                normalizedUsername,
                profile.getPlayerId(),
                hashPassword(password),
                sessionToken,
                Instant.now()
        );
        accountRepository.save(account);

        return toSession(account, profile);
    }

    public AuthSession login(String username, String password) {
        String normalizedUsername = validateUsername(username);
        UserAccount account = accountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AuthenticationFailedException("bad credentials"));
        if (!verifyPassword(password, account.getPasswordHash())) {
            throw new AuthenticationFailedException("bad credentials");
        }

        String sessionToken = newSessionToken();
        account.refreshSessionToken(sessionToken);
        accountRepository.save(account);

        PlayerProfile profile = playerRepository.find(account.getPlayerId())
                .orElseThrow(() -> new AuthenticationFailedException("player profile missing"));
        profile.convertToAccount(account.getUsername(), sessionToken);
        playerRepository.save(profile);

        return toSession(account, profile);
    }

    public void logout(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        accountRepository.findBySessionToken(sessionToken).ifPresent(account -> {
            account.clearSessionToken();
            accountRepository.save(account);
        });
    }

    private PlayerProfile resolveRegistrationProfile(String username, String playerId, String sessionToken) {
        if (playerId == null || playerId.isBlank()) {
            return PlayerProfile.createAccount(username, sessionToken);
        }
        PlayerProfile profile = playerRepository.find(playerId)
                .orElseThrow(() -> new IllegalArgumentException("player not found"));
        profile.convertToAccount(username, sessionToken);
        return profile;
    }

    private AuthSession toSession(UserAccount account, PlayerProfile profile) {
        return new AuthSession(
                profile.getPlayerId(),
                account.getUsername(),
                profile.getDisplayName(),
                account.getSessionToken()
        );
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        String normalized = username.trim();
        if (normalized.length() > 32) {
            throw new IllegalArgumentException("username is too long");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password is too short");
        }
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS);
        return "pbkdf2$" + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String password, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
        byte[] actualHash = pbkdf2(password, salt, iterations);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("password hashing unavailable", e);
        }
    }

    private String newSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}