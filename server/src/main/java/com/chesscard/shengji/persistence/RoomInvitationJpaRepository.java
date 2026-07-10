package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.InvitationStatus;
import com.chesscard.shengji.domain.RoomInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomInvitationJpaRepository extends JpaRepository<RoomInvitation, String> {
    List<RoomInvitation> findByToPlayerIdAndStatusOrderByCreatedAtDesc(String toPlayerId, InvitationStatus status);
}