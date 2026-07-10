package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Friendship;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FriendService {
    private final FriendshipRepository repository;

    public FriendService(FriendshipRepository repository) {
        this.repository = repository;
    }

    public Friendship sendRequest(String requesterPlayerId, String addresseePlayerId) {
        requireId(requesterPlayerId, "requesterPlayerId");
        requireId(addresseePlayerId, "addresseePlayerId");
        if (requesterPlayerId.equals(addresseePlayerId)) {
            throw new IllegalArgumentException("cannot add yourself as a friend");
        }
        repository.findBetweenPlayers(requesterPlayerId, addresseePlayerId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("friendship already exists");
                });
        return repository.save(Friendship.pending(requesterPlayerId, addresseePlayerId, Instant.now()));
    }

    public Friendship acceptRequest(String friendshipId, String playerId) {
        requireId(friendshipId, "friendshipId");
        requireId(playerId, "playerId");
        Friendship friendship = repository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("friendship not found"));
        return repository.save(friendship.accept(playerId));
    }

    public List<Friendship> listFriends(String playerId) {
        requireId(playerId, "playerId");
        return repository.findAcceptedForPlayer(playerId);
    }

    public void deleteFriendship(String friendshipId, String playerId) {
        requireId(friendshipId, "friendshipId");
        requireId(playerId, "playerId");
        Friendship friendship = repository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("friendship not found"));
        if (!friendship.involves(playerId)) {
            throw new IllegalArgumentException("player is not part of friendship");
        }
        repository.delete(friendship);
    }

    private void requireId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}