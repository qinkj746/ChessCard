package com.chesscard.shengji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "room_invitation")
public class RoomInvitation {
    private static final long EXPIRATION_MINUTES = 30;

    @Id
    @Column(length = 36)
    private String invitationId;

    @Column(nullable = false, length = 36)
    private String roomId;

    @Column(nullable = false, length = 36)
    private String fromPlayerId;

    @Column(nullable = false, length = 36)
    private String toPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RoomInvitation() {
    }

    public RoomInvitation(
            String invitationId,
            String roomId,
            String fromPlayerId,
            String toPlayerId,
            InvitationStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.invitationId = invitationId;
        this.roomId = roomId;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static RoomInvitation create(String roomId, String fromPlayerId, String toPlayerId, Instant now) {
        return new RoomInvitation(
                UUID.randomUUID().toString(),
                roomId,
                fromPlayerId,
                toPlayerId,
                InvitationStatus.PENDING,
                now,
                now.plusSeconds(EXPIRATION_MINUTES * 60)
        );
    }

    public RoomInvitation accept(String playerId) {
        requireTarget(playerId);
        status = InvitationStatus.ACCEPTED;
        return this;
    }

    public RoomInvitation decline(String playerId) {
        requireTarget(playerId);
        status = InvitationStatus.DECLINED;
        return this;
    }

    private void requireTarget(String playerId) {
        if (!Objects.equals(toPlayerId, playerId)) {
            throw new IllegalArgumentException("only target player can respond to invitation");
        }
    }

    public String getInvitationId() {
        return invitationId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getFromPlayerId() {
        return fromPlayerId;
    }

    public String getToPlayerId() {
        return toPlayerId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}