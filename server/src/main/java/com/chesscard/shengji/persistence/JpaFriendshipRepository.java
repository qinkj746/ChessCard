package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.FriendshipStatus;
import com.chesscard.shengji.service.FriendshipRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaFriendshipRepository implements FriendshipRepository {
    private final FriendshipJpaRepository jpaRepository;

    public JpaFriendshipRepository(FriendshipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Friendship save(Friendship friendship) {
        return jpaRepository.save(friendship);
    }

    @Override
    public Optional<Friendship> findById(String friendshipId) {
        return jpaRepository.findById(friendshipId);
    }

    @Override
    public Optional<Friendship> findBetweenPlayers(String playerA, String playerB) {
        return jpaRepository.findByRequesterPlayerIdAndAddresseePlayerId(playerA, playerB)
                .or(() -> jpaRepository.findByRequesterPlayerIdAndAddresseePlayerId(playerB, playerA));
    }

    @Override
    public List<Friendship> findAcceptedForPlayer(String playerId) {
        return jpaRepository.findByStatusAndRequesterPlayerIdOrStatusAndAddresseePlayerId(
                FriendshipStatus.ACCEPTED,
                playerId,
                FriendshipStatus.ACCEPTED,
                playerId
        );
    }

    @Override
    public void delete(Friendship friendship) {
        jpaRepository.delete(friendship);
    }
}