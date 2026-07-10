package com.chesscard.shengji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_profile")
public class PlayerProfile {
    @Id
    @Column(length = 36)
    private String playerId;

    @Column(nullable = false, length = 32)
    private String displayName;

    @Column(nullable = false)
    private boolean guest;

    @Column(unique = true, length = 64)
    private String sessionToken;

    @Column(nullable = false)
    private Instant createdAt;

    protected PlayerProfile() {
    }

    public PlayerProfile(String playerId, String displayName, boolean guest, String sessionToken, Instant createdAt) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.guest = guest;
        this.sessionToken = sessionToken;
        this.createdAt = createdAt;
    }

    public static PlayerProfile createGuest() {
        String id = UUID.randomUUID().toString();
        int suffix = (id.hashCode() & 0x7FFFFFFF) % 100000;
        String name = "Guest-" + String.format("%05d", suffix);
        return new PlayerProfile(id, name, true, UUID.randomUUID().toString().replace("-", ""), Instant.now());
    }

    public static PlayerProfile createAccount(String displayName, String sessionToken) {
        return new PlayerProfile(UUID.randomUUID().toString(), displayName, false, sessionToken, Instant.now());
    }

    public void convertToAccount(String displayName, String sessionToken) {
        this.displayName = displayName;
        this.guest = false;
        this.sessionToken = sessionToken;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isGuest() {
        return guest;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}