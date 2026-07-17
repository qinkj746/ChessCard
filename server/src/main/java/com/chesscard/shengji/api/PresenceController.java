package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.HeartbeatRequest;
import com.chesscard.shengji.api.dto.OnlinePlayerDto;
import com.chesscard.shengji.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
@CrossOrigin(origins = "*")
public class PresenceController {
    private final PlayerService service;

    public PresenceController(PlayerService service) {
        this.service = service;
    }

    @PostMapping("/heartbeat")
    public OnlinePlayerDto heartbeat(@RequestBody HeartbeatRequest request) {
        return OnlinePlayerDto.from(service.markOnline(request.playerId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_OPERATION", ex.getMessage()));
    }
}
