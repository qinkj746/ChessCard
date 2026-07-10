package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.InvitationStatus;
import com.chesscard.shengji.domain.RoomInvitation;
import com.chesscard.shengji.service.RoomInvitationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaRoomInvitationRepository implements RoomInvitationRepository {
    private final RoomInvitationJpaRepository jpaRepository;

    public JpaRoomInvitationRepository(RoomInvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RoomInvitation save(RoomInvitation invitation) {
        return jpaRepository.save(invitation);
    }

    @Override
    public Optional<RoomInvitation> findById(String invitationId) {
        return jpaRepository.findById(invitationId);
    }

    @Override
    public List<RoomInvitation> findPendingForPlayer(String playerId) {
        return jpaRepository.findByToPlayerIdAndStatusOrderByCreatedAtDesc(playerId, InvitationStatus.PENDING);
    }
}