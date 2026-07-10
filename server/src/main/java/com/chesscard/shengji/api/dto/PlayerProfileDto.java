package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.PlayerProfile;

public record PlayerProfileDto(String playerId, String displayName, boolean guest, String sessionToken) {
    public static PlayerProfileDto from(PlayerProfile profile) {
        return new PlayerProfileDto(
                profile.getPlayerId(),
                profile.getDisplayName(),
                profile.isGuest(),
                profile.getSessionToken()
        );
    }
}