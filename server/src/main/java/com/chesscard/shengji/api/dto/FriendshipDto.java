package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.FriendshipStatus;

import java.time.Instant;

public record FriendshipDto(
        String friendshipId,
        String requesterPlayerId,
        String addresseePlayerId,
        FriendshipStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static FriendshipDto from(Friendship friendship) {
        return new FriendshipDto(
                friendship.getFriendshipId(),
                friendship.getRequesterPlayerId(),
                friendship.getAddresseePlayerId(),
                friendship.getStatus(),
                friendship.getCreatedAt(),
                friendship.getUpdatedAt()
        );
    }
}