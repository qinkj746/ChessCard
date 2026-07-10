package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameNotFoundException;
import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.websocket.RoomEventType;
import com.chesscard.shengji.domain.FriendshipStatus;
import com.chesscard.shengji.domain.RoomInvitation;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class InvitationService {
    private final RoomInvitationRepository invitationRepository;
    private final RoomRepository roomRepository;
    private final FriendshipRepository friendshipRepository;
    private final RoomEventPublisher eventPublisher;

    public InvitationService(
            RoomInvitationRepository invitationRepository,
            RoomRepository roomRepository,
            FriendshipRepository friendshipRepository,
            RoomEventPublisher eventPublisher
    ) {
        this.invitationRepository = invitationRepository;
        this.roomRepository = roomRepository;
        this.friendshipRepository = friendshipRepository;
        this.eventPublisher = eventPublisher;
    }

    public RoomInvitation createInvitation(String roomId, String fromPlayerId, String toPlayerId) {
        requireId(roomId, "roomId");
        requireId(fromPlayerId, "fromPlayerId");
        requireId(toPlayerId, "toPlayerId");
        RoomState room = roomRepository.find(roomId)
                .orElseThrow(() -> new GameNotFoundException("room not found"));
        if (!isRoomMember(room, fromPlayerId)) {
            throw new PermissionDeniedException("inviter is not in room");
        }
        boolean acceptedFriend = friendshipRepository.findBetweenPlayers(fromPlayerId, toPlayerId)
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                .isPresent();
        if (!acceptedFriend) {
            throw new PermissionDeniedException("target player is not a friend");
        }
        RoomInvitation saved = invitationRepository.save(
                RoomInvitation.create(room.getRoomId(), fromPlayerId, toPlayerId, Instant.now())
        );
        eventPublisher.publish(new RoomEvent(RoomEventType.ROOM_INVITATION, room.getRoomId(), null, null, saved));
        return saved;
    }

    public List<RoomInvitation> listPendingInvitations(String playerId) {
        requireId(playerId, "playerId");
        return invitationRepository.findPendingForPlayer(playerId);
    }

    public RoomInvitation respondToInvitation(String invitationId, String playerId, boolean accepted) {
        requireId(invitationId, "invitationId");
        requireId(playerId, "playerId");
        RoomInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("invitation not found"));
        return invitationRepository.save(accepted ? invitation.accept(playerId) : invitation.decline(playerId));
    }

    private boolean isRoomMember(RoomState room, String playerId) {
        return room.getSeats().values().stream()
                .map(RoomSeat::getPlayerId)
                .anyMatch(playerId::equals);
    }

    private void requireId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}