package com.chesscard.shengji.domain;

public record AuthSession(String playerId, String username, String displayName, String sessionToken) {
}