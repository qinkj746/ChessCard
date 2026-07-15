package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CreateRoomRequest;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.GameStateDto;
import com.chesscard.shengji.api.dto.JoinSeatRequest;
import com.chesscard.shengji.api.dto.RoomStateDto;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.PlayerService;
import com.chesscard.shengji.service.RoomService;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {
    private final RoomService service;
    private final PlayerService playerService;

    public RoomController(RoomService service, PlayerService playerService) {
        this.service = service;
        this.playerService = playerService;
    }

    @PostMapping
    public RoomStateDto create(@RequestBody CreateRoomRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        return toDto(service.createRoom(request.playerId()));
    }

    @GetMapping("/{id}")
    public RoomStateDto get(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("roomId 不能为空");
        }
        return toDto(service.getRoom(id));
    }

    @PostMapping("/{id}/seats/{seat}")
    public RoomStateDto joinSeat(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        String playerId = requirePlayerRequest(request);
        PlayerSeat playerSeat = parseSeat(seat);
        return toDto(service.joinSeat(id, playerId, playerSeat));
    }

    @DeleteMapping("/{id}/seats/{seat}")
    public RoomStateDto leaveSeat(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        String playerId = requirePlayerRequest(request);
        PlayerSeat playerSeat = parseSeat(seat);
        return toDto(service.leaveSeat(id, playerId, playerSeat));
    }

    @PostMapping("/{id}/seats/{seat}/bot")
    public RoomStateDto addBot(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        String playerId = requirePlayerRequest(request);
        return toDto(service.addBot(id, playerId, parseSeat(seat)));
    }

    @DeleteMapping("/{id}/seats/{seat}/bot")
    public RoomStateDto removeBot(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        String playerId = requirePlayerRequest(request);
        return toDto(service.removeBot(id, playerId, parseSeat(seat)));
    }

    @PostMapping("/{id}/start")
    public GameStateDto start(@PathVariable String id, @RequestBody JoinSeatRequest request) {
        String playerId = requirePlayerRequest(request);
        return GameStateDto.from(service.startGame(id, playerId));
    }

    private String requirePlayerRequest(JoinSeatRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        return request.playerId();
    }

    private PlayerSeat parseSeat(String seat) {
        try {
            return PlayerSeat.valueOf(seat.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效座位: " + seat);
        }
    }

    private RoomStateDto toDto(RoomState room) {
        return RoomStateDto.from(room, this::displayNameFor);
    }

    private String displayNameFor(String playerId) {
        try {
            String displayName = playerService.getPlayer(playerId).getDisplayName();
            return displayName == null || displayName.isBlank() ? playerId : displayName;
        } catch (IllegalArgumentException e) {
            return playerId;
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_OPERATION", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("PERMISSION_DENIED", ex.getMessage()));
    }
}
