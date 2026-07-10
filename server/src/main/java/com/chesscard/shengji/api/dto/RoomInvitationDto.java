package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.InvitationStatus;
import com.chesscard.shengji.domain.RoomInvitation;

import java.time.Instant;

public record RoomInvitationDto(
        String invitationId,
        String roomId,
        String fromPlayerId,
        String toPlayerId,
        InvitationStatus status,
        Instant createdAt,
        Instant expiresAt
) {
    public static RoomInvitationDto from(RoomInvitation invitation) {
        return new RoomInvitationDto(
                invitation.getInvitationId(),
                invitation.getRoomId(),
                invitation.getFromPlayerId(),
                invitation.getToPlayerId(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt()
        );
    }
}