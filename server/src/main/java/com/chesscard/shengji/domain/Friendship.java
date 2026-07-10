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
@Table(name = "friendship")
public class Friendship {
    @Id
    @Column(length = 36)
    private String friendshipId;

    @Column(nullable = false, length = 36)
    private String requesterPlayerId;

    @Column(nullable = false, length = 36)
    private String addresseePlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Friendship() {
    }

    public Friendship(
            String friendshipId,
            String requesterPlayerId,
            String addresseePlayerId,
            FriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.friendshipId = friendshipId;
        this.requesterPlayerId = requesterPlayerId;
        this.addresseePlayerId = addresseePlayerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Friendship pending(String requesterPlayerId, String addresseePlayerId, Instant now) {
        return new Friendship(
                UUID.randomUUID().toString(),
                requesterPlayerId,
                addresseePlayerId,
                FriendshipStatus.PENDING,
                now,
                now
        );
    }

    public static Friendship accepted(String friendshipId, String requesterPlayerId, String addresseePlayerId, Instant now) {
        return new Friendship(
                friendshipId,
                requesterPlayerId,
                addresseePlayerId,
                FriendshipStatus.ACCEPTED,
                now,
                now
        );
    }

    public Friendship accept(String playerId) {
        if (!Objects.equals(addresseePlayerId, playerId)) {
            throw new IllegalArgumentException("only addressee can accept friendship");
        }
        status = FriendshipStatus.ACCEPTED;
        updatedAt = Instant.now();
        return this;
    }

    public boolean connects(String playerA, String playerB) {
        return (Objects.equals(requesterPlayerId, playerA) && Objects.equals(addresseePlayerId, playerB))
                || (Objects.equals(requesterPlayerId, playerB) && Objects.equals(addresseePlayerId, playerA));
    }

    public boolean involves(String playerId) {
        return Objects.equals(requesterPlayerId, playerId) || Objects.equals(addresseePlayerId, playerId);
    }

    public String getFriendshipId() {
        return friendshipId;
    }

    public String getRequesterPlayerId() {
        return requesterPlayerId;
    }

    public String getAddresseePlayerId() {
        return addresseePlayerId;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}