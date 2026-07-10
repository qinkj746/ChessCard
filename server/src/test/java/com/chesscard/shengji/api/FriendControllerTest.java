package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.AcceptFriendRequest;
import com.chesscard.shengji.api.dto.CreateFriendRequest;
import com.chesscard.shengji.api.dto.CreateInvitationRequest;
import com.chesscard.shengji.api.dto.FriendshipDto;
import com.chesscard.shengji.api.dto.PlayerRequest;
import com.chesscard.shengji.api.dto.RespondInvitationRequest;
import com.chesscard.shengji.api.dto.RoomInvitationDto;
import com.chesscard.shengji.domain.Friendship;
import com.chesscard.shengji.domain.FriendshipStatus;
import com.chesscard.shengji.domain.RoomInvitation;
import com.chesscard.shengji.service.FriendService;
import com.chesscard.shengji.service.InvitationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FriendControllerTest {
    private final StubFriendService friendService = new StubFriendService();
    private final StubInvitationService invitationService = new StubInvitationService();
    private final FriendController controller = new FriendController(friendService, invitationService);

    @Test
    void sendFriendRequestReturnsFriendshipDto() {
        FriendshipDto dto = controller.sendFriendRequest(new CreateFriendRequest("player-a", "player-b"));

        assertThat(friendService.requester).isEqualTo("player-a");
        assertThat(friendService.addressee).isEqualTo("player-b");
        assertThat(dto.friendshipId()).isEqualTo("friendship-1");
        assertThat(dto.status()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    void acceptFriendRequestReturnsAcceptedFriendshipDto() {
        FriendshipDto dto = controller.acceptFriendRequest("friendship-1", new AcceptFriendRequest("player-b"));

        assertThat(friendService.acceptedBy).isEqualTo("player-b");
        assertThat(dto.status()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void deleteFriendshipDelegatesToService() {
        controller.deleteFriendship("friendship-1", new PlayerRequest("player-a"));

        assertThat(friendService.deletedBy).isEqualTo("player-a");
    }

    @Test
    void listFriendsReturnsFriendshipDtos() {
        List<FriendshipDto> friends = controller.listFriends("player-a");

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).friendshipId()).isEqualTo("friendship-accepted");
    }

    @Test
    void createInvitationReturnsInvitationDto() {
        RoomInvitationDto dto = controller.createInvitation(
                "room-1",
                new CreateInvitationRequest("player-a", "player-b")
        );

        assertThat(invitationService.roomId).isEqualTo("room-1");
        assertThat(invitationService.fromPlayerId).isEqualTo("player-a");
        assertThat(invitationService.toPlayerId).isEqualTo("player-b");
        assertThat(dto.invitationId()).isEqualTo("invitation-1");
        assertThat(dto.roomId()).isEqualTo("room-1");
    }

    @Test
    void listPendingInvitationsReturnsInvitationDtos() {
        List<RoomInvitationDto> invitations = controller.listPendingInvitations("player-b");

        assertThat(invitations).hasSize(1);
        assertThat(invitations.get(0).invitationId()).isEqualTo("invitation-1");
    }

    @Test
    void respondToInvitationReturnsUpdatedInvitationDto() {
        RoomInvitationDto dto = controller.respondToInvitation(
                "invitation-1",
                new RespondInvitationRequest("player-b", true)
        );

        assertThat(invitationService.respondedBy).isEqualTo("player-b");
        assertThat(invitationService.responseAccepted).isTrue();
        assertThat(dto.status().name()).isEqualTo("ACCEPTED");
    }

    @Test
    void permissionFailureMapsToForbidden() {
        var response = controller.forbidden(new PermissionDeniedException("not allowed"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().code()).isEqualTo("PERMISSION_DENIED");
    }

    private static Friendship pendingFriendship() {
        Instant now = Instant.parse("2026-07-10T08:00:00Z");
        return new Friendship("friendship-1", "player-a", "player-b", FriendshipStatus.PENDING, now, now);
    }

    private static Friendship acceptedFriendship() {
        return Friendship.accepted("friendship-accepted", "player-a", "player-b", Instant.parse("2026-07-10T08:00:00Z"));
    }

    private static RoomInvitation invitation() {
        Instant now = Instant.parse("2026-07-10T08:00:00Z");
        return new RoomInvitation("invitation-1", "room-1", "player-a", "player-b", com.chesscard.shengji.domain.InvitationStatus.PENDING, now, now.plusSeconds(1800));
    }

    private static class StubFriendService extends FriendService {
        String requester;
        String addressee;
        String acceptedBy;
        String deletedBy;

        StubFriendService() {
            super(null);
        }

        @Override
        public Friendship sendRequest(String requesterPlayerId, String addresseePlayerId) {
            requester = requesterPlayerId;
            addressee = addresseePlayerId;
            return pendingFriendship();
        }

        @Override
        public Friendship acceptRequest(String friendshipId, String playerId) {
            acceptedBy = playerId;
            return acceptedFriendship();
        }

        @Override
        public List<Friendship> listFriends(String playerId) {
            return List.of(acceptedFriendship());
        }

        @Override
        public void deleteFriendship(String friendshipId, String playerId) {
            deletedBy = playerId;
        }
    }

    private static class StubInvitationService extends InvitationService {
        String roomId;
        String fromPlayerId;
        String toPlayerId;
        String respondedBy;
        boolean responseAccepted;

        StubInvitationService() {
            super(null, null, null, null);
        }

        @Override
        public RoomInvitation createInvitation(String roomId, String fromPlayerId, String toPlayerId) {
            this.roomId = roomId;
            this.fromPlayerId = fromPlayerId;
            this.toPlayerId = toPlayerId;
            return invitation();
        }

        @Override
        public List<RoomInvitation> listPendingInvitations(String playerId) {
            return List.of(invitation());
        }

        @Override
        public RoomInvitation respondToInvitation(String invitationId, String playerId, boolean accepted) {
            respondedBy = playerId;
            responseAccepted = accepted;
            RoomInvitation invitation = invitation();
            invitation.accept(playerId);
            return invitation;
        }
    }
}