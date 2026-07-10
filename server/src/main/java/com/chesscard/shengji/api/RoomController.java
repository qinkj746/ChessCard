package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CreateRoomRequest;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.GameStateDto;
import com.chesscard.shengji.api.dto.JoinSeatRequest;
import com.chesscard.shengji.api.dto.RoomStateDto;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.service.RoomService;
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

    public RoomController(RoomService service) {
        this.service = service;
    }

    @PostMapping
    public RoomStateDto create(@RequestBody CreateRoomRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        return RoomStateDto.from(service.createRoom(request.playerId()));
    }

    @GetMapping("/{id}")
    public RoomStateDto get(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("roomId 不能为空");
        }
        return RoomStateDto.from(service.getRoom(id));
    }

    @PostMapping("/{id}/seats/{seat}")
    public RoomStateDto joinSeat(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        PlayerSeat playerSeat = parseSeat(seat);
        return RoomStateDto.from(service.joinSeat(id, request.playerId(), playerSeat));
    }

    @DeleteMapping("/{id}/seats/{seat}")
    public RoomStateDto leaveSeat(@PathVariable String id, @PathVariable String seat, @RequestBody JoinSeatRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        PlayerSeat playerSeat = parseSeat(seat);
        return RoomStateDto.from(service.leaveSeat(id, request.playerId(), playerSeat));
    }

    @PostMapping("/{id}/start")
    public GameStateDto start(@PathVariable String id, @RequestBody JoinSeatRequest request) {
        if (request == null || request.playerId() == null || request.playerId().isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        return GameStateDto.from(service.startGame(id, request.playerId()));
    }

    private PlayerSeat parseSeat(String seat) {
        try {
            return PlayerSeat.valueOf(seat.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效座位: " + seat);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_OPERATION", ex.getMessage()));
    }
}
