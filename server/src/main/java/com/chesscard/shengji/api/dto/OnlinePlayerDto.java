package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.PlayerProfile;

import java.time.Instant;

public record OnlinePlayerDto(String playerId, String displayName, boolean guest, Instant lastSeenAt) {
    public static OnlinePlayerDto from(PlayerProfile profile) {
        return new OnlinePlayerDto(
                profile.getPlayerId(),
                profile.getDisplayName(),
                profile.isGuest(),
                profile.getLastSeenAt()
        );
    }
}
