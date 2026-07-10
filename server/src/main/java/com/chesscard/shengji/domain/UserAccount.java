package com.chesscard.shengji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id
    @Column(length = 32)
    private String username;

    @Column(nullable = false, length = 36)
    private String playerId;

    @Column(nullable = false, length = 256)
    private String passwordHash;

    @Column(unique = true, length = 64)
    private String sessionToken;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserAccount() {
    }

    public UserAccount(String username, String playerId, String passwordHash, String sessionToken, Instant createdAt) {
        this.username = username;
        this.playerId = playerId;
        this.passwordHash = passwordHash;
        this.sessionToken = sessionToken;
        this.createdAt = createdAt;
    }

    public void refreshSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void clearSessionToken() {
        this.sessionToken = null;
    }

    public String getUsername() {
        return username;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}