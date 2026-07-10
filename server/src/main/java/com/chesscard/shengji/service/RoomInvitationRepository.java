package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.RoomInvitation;

import java.util.List;
import java.util.Optional;

public interface RoomInvitationRepository {
    RoomInvitation save(RoomInvitation invitation);

    Optional<RoomInvitation> findById(String invitationId);

    List<RoomInvitation> findPendingForPlayer(String playerId);
}