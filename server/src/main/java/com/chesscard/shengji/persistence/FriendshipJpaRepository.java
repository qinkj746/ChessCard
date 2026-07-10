package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipJpaRepository extends JpaRepository<Friendship, String> {
    Optional<Friendship> findByRequesterPlayerIdAndAddresseePlayerId(String requesterPlayerId, String addresseePlayerId);

    List<Friendship> findByStatusAndRequesterPlayerIdOrStatusAndAddresseePlayerId(
            FriendshipStatus requesterStatus,
            String requesterPlayerId,
            FriendshipStatus addresseeStatus,
            String addresseePlayerId
    );
}