package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository {
    Friendship save(Friendship friendship);

    Optional<Friendship> findById(String friendshipId);

    Optional<Friendship> findBetweenPlayers(String playerA, String playerB);

    List<Friendship> findAcceptedForPlayer(String playerId);

    void delete(Friendship friendship);
}