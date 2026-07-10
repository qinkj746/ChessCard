package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.AuthSession;

public record AuthSessionDto(String playerId, String username, String displayName, String sessionToken) {
    public static AuthSessionDto from(AuthSession session) {
        return new AuthSessionDto(
                session.playerId(),
                session.username(),
                session.displayName(),
                session.sessionToken()
        );
    }
}