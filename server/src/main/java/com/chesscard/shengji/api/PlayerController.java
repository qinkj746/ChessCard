package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.PlayerProfileDto;
import com.chesscard.shengji.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @PostMapping("/guest")
    public PlayerProfileDto createGuest() {
        return PlayerProfileDto.from(service.createGuest());
    }

    @PostMapping("/{id}/refresh")
    public PlayerProfileDto refresh(@PathVariable String id) {
        return PlayerProfileDto.from(service.getPlayer(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_OPERATION", ex.getMessage()));
    }
}
