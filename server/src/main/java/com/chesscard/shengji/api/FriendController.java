package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.AcceptFriendRequest;
import com.chesscard.shengji.api.dto.CreateFriendRequest;
import com.chesscard.shengji.api.dto.CreateInvitationRequest;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.FriendshipDto;
import com.chesscard.shengji.api.dto.PlayerRequest;
import com.chesscard.shengji.api.dto.RespondInvitationRequest;
import com.chesscard.shengji.api.dto.RoomInvitationDto;
import com.chesscard.shengji.service.FriendService;
import com.chesscard.shengji.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FriendController {
    private final FriendService friendService;
    private final InvitationService invitationService;

    public FriendController(FriendService friendService, InvitationService invitationService) {
        this.friendService = friendService;
        this.invitationService = invitationService;
    }

    @PostMapping("/friends/requests")
    public FriendshipDto sendFriendRequest(@RequestBody CreateFriendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return FriendshipDto.from(friendService.sendRequest(request.requesterPlayerId(), request.addresseePlayerId()));
    }

    @PostMapping("/friends/requests/{friendshipId}/accept")
    public FriendshipDto acceptFriendRequest(
            @PathVariable String friendshipId,
            @RequestBody AcceptFriendRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return FriendshipDto.from(friendService.acceptRequest(friendshipId, request.playerId()));
    }

    @DeleteMapping("/friends/{friendshipId}")
    public ResponseEntity<Void> deleteFriendship(
            @PathVariable String friendshipId,
            @RequestBody PlayerRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        friendService.deleteFriendship(friendshipId, request.playerId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/players/{playerId}/friends")
    public List<FriendshipDto> listFriends(@PathVariable String playerId) {
        return friendService.listFriends(playerId).stream().map(FriendshipDto::from).toList();
    }

    @PostMapping("/rooms/{roomId}/invitations")
    public RoomInvitationDto createInvitation(
            @PathVariable String roomId,
            @RequestBody CreateInvitationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return RoomInvitationDto.from(invitationService.createInvitation(roomId, request.fromPlayerId(), request.toPlayerId()));
    }

    @GetMapping("/players/{playerId}/invitations")
    public List<RoomInvitationDto> listPendingInvitations(@PathVariable String playerId) {
        return invitationService.listPendingInvitations(playerId).stream().map(RoomInvitationDto::from).toList();
    }

    @PostMapping("/invitations/{invitationId}/respond")
    public RoomInvitationDto respondToInvitation(
            @PathVariable String invitationId,
            @RequestBody RespondInvitationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return RoomInvitationDto.from(
                invitationService.respondToInvitation(invitationId, request.playerId(), request.accepted())
        );
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(PermissionDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of("PERMISSION_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(GameNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of("ROOM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_FRIEND_OPERATION", ex.getMessage()));
    }
}