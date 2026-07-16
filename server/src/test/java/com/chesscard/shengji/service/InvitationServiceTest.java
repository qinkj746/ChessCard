package com.chesscard.shengji.service;

import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.websocket.RoomEventType;
import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.InvitationStatus;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomInvitation;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationServiceTest {
    private final FakeRoomInvitationRepository invitationRepository = new FakeRoomInvitationRepository();
    private final FakeRoomRepository roomRepository = new FakeRoomRepository();
    private final FakeFriendshipRepository friendshipRepository = new FakeFriendshipRepository();
    private final CapturingPublisher publisher = new CapturingPublisher();
    private final InvitationService service = new InvitationService(
            invitationRepository,
            roomRepository,
            friendshipRepository,
            publisher
    );

    @Test
    void createInvitationStoresInvitationAndPublishesEvent() {
        roomRepository.save(roomWithPlayer("room-1", "player-a"));
        friendshipRepository.save(Friendship.accepted("friendship-1", "player-a", "player-b", Instant.now()));

        RoomInvitation invitation = service.createInvitation("room-1", "player-a", "player-b");

        assertThat(invitation.getInvitationId()).isNotBlank();
        assertThat(invitation.getRoomId()).isEqualTo("room-1");
        assertThat(invitation.getFromPlayerId()).isEqualTo("player-a");
        assertThat(invitation.getToPlayerId()).isEqualTo("player-b");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getExpiresAt()).isAfter(invitation.getCreatedAt());
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).type()).isEqualTo(RoomEventType.ROOM_INVITATION);
        assertThat(publisher.events.get(0).payload()).isEqualTo(invitation);
    }

    @Test
    void createInvitationRejectsNonRoomMember() {
        roomRepository.save(roomWithPlayer("room-1", "player-a"));
        friendshipRepository.save(Friendship.accepted("friendship-1", "player-a", "player-b", Instant.now()));

        assertThatThrownBy(() -> service.createInvitation("room-1", "player-c", "player-b"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void createInvitationRejectsNonFriend() {
        roomRepository.save(roomWithPlayer("room-1", "player-a"));

        assertThatThrownBy(() -> service.createInvitation("room-1", "player-a", "player-b"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void listPendingInvitationsReturnsOnlyTargetPendingInvitations() {
        RoomInvitation pending = RoomInvitation.create("room-1", "player-a", "player-b", Instant.now());
        RoomInvitation declined = RoomInvitation.create("room-1", "player-a", "player-b", Instant.now());
        declined.decline("player-b");
        invitationRepository.save(pending);
        invitationRepository.save(declined);
        invitationRepository.save(RoomInvitation.create("room-1", "player-a", "player-c", Instant.now()));

        assertThat(service.listPendingInvitations("player-b")).containsExactly(pending);
    }

    @Test
    void respondToInvitationAcceptsOrDeclinesByTargetPlayer() {
        RoomInvitation invitation = RoomInvitation.create("room-1", "player-a", "player-b", Instant.now());
        invitationRepository.save(invitation);

        RoomInvitation accepted = service.respondToInvitation(invitation.getInvitationId(), "player-b", true);

        assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    private static RoomState roomWithPlayer(String roomId, String playerId) {
        RoomState room = new RoomState();
        room.setRoomId(roomId);
        room.getSeats().put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, playerId, Instant.now()));
        return room;
    }

    private static class FakeRoomInvitationRepository implements RoomInvitationRepository {
        final List<RoomInvitation> invitations = new ArrayList<>();

        @Override
        public RoomInvitation save(RoomInvitation invitation) {
            invitations.removeIf(existing -> existing.getInvitationId().equals(invitation.getInvitationId()));
            invitations.add(invitation);
            return invitation;
        }

        @Override
        public Optional<RoomInvitation> findById(String invitationId) {
            return invitations.stream()
                    .filter(invitation -> invitation.getInvitationId().equals(invitationId))
                    .findFirst();
        }

        @Override
        public List<RoomInvitation> findPendingForPlayer(String playerId) {
            return invitations.stream()
                    .filter(invitation -> invitation.getToPlayerId().equals(playerId))
                    .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                    .toList();
        }
    }

    private static class FakeRoomRepository implements RoomRepository {
        final Map<String, RoomState> rooms = new HashMap<>();

        @Override
        public RoomState save(RoomState room) {
            rooms.put(room.getRoomId(), room);
            return room;
        }

        @Override
        public void delete(String id) {
            rooms.remove(id);
        }

        @Override
        public Optional<RoomState> find(String id) {
            return Optional.ofNullable(rooms.get(id));
        }

    }

    private static class FakeFriendshipRepository implements FriendshipRepository {
        final List<Friendship> friendships = new ArrayList<>();

        @Override
        public Friendship save(Friendship friendship) {
            friendships.add(friendship);
            return friendship;
        }

        @Override
        public Optional<Friendship> findById(String friendshipId) {
            return friendships.stream().filter(item -> item.getFriendshipId().equals(friendshipId)).findFirst();
        }

        @Override
        public Optional<Friendship> findBetweenPlayers(String playerA, String playerB) {
            return friendships.stream().filter(item -> item.connects(playerA, playerB)).findFirst();
        }

        @Override
        public List<Friendship> findAcceptedForPlayer(String playerId) {
            return friendships.stream().filter(item -> item.involves(playerId)).toList();
        }

        @Override
        public void delete(Friendship friendship) {
            friendships.remove(friendship);
        }
    }

    private static class CapturingPublisher implements RoomEventPublisher {
        final List<RoomEvent> events = new ArrayList<>();

        @Override
        public void publish(RoomEvent event) {
            events.add(event);
        }
    }
}
