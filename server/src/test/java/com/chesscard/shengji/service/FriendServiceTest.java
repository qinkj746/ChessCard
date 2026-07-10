package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.FriendshipStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendServiceTest {
    private final FakeFriendshipRepository repository = new FakeFriendshipRepository();
    private final FriendService service = new FriendService(repository);

    @Test
    void sendRequestCreatesPendingFriendship() {
        Friendship friendship = service.sendRequest("player-a", "player-b");

        assertThat(friendship.getFriendshipId()).isNotBlank();
        assertThat(friendship.getRequesterPlayerId()).isEqualTo("player-a");
        assertThat(friendship.getAddresseePlayerId()).isEqualTo("player-b");
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    void acceptRequestMakesFriendshipVisibleToBothPlayers() {
        Friendship request = service.sendRequest("player-a", "player-b");

        Friendship accepted = service.acceptRequest(request.getFriendshipId(), "player-b");

        assertThat(accepted.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(service.listFriends("player-a")).containsExactly(accepted);
        assertThat(service.listFriends("player-b")).containsExactly(accepted);
    }

    @Test
    void deleteFriendshipRemovesFriendshipForBothPlayers() {
        Friendship request = service.sendRequest("player-a", "player-b");
        Friendship accepted = service.acceptRequest(request.getFriendshipId(), "player-b");

        service.deleteFriendship(accepted.getFriendshipId(), "player-a");

        assertThat(service.listFriends("player-a")).isEmpty();
        assertThat(service.listFriends("player-b")).isEmpty();
    }

    @Test
    void sendRequestRejectsSelfFriendship() {
        assertThatThrownBy(() -> service.sendRequest("player-a", "player-a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class FakeFriendshipRepository implements FriendshipRepository {
        final List<Friendship> friendships = new ArrayList<>();

        @Override
        public Friendship save(Friendship friendship) {
            friendships.removeIf(existing -> existing.getFriendshipId().equals(friendship.getFriendshipId()));
            friendships.add(friendship);
            return friendship;
        }

        @Override
        public Optional<Friendship> findById(String friendshipId) {
            return friendships.stream()
                    .filter(friendship -> friendship.getFriendshipId().equals(friendshipId))
                    .findFirst();
        }

        @Override
        public Optional<Friendship> findBetweenPlayers(String playerA, String playerB) {
            return friendships.stream()
                    .filter(friendship -> friendship.connects(playerA, playerB))
                    .findFirst();
        }

        @Override
        public List<Friendship> findAcceptedForPlayer(String playerId) {
            return friendships.stream()
                    .filter(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                    .filter(friendship -> friendship.involves(playerId))
                    .toList();
        }

        @Override
        public void delete(Friendship friendship) {
            friendships.remove(friendship);
        }
    }
}