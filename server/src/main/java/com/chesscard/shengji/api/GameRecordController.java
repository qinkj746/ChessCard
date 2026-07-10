package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.GameRecordDto;
import com.chesscard.shengji.service.GameRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GameRecordController {
    private final GameRecordService service;

    public GameRecordController(GameRecordService service) {
        this.service = service;
    }

    @GetMapping("/players/{playerId}/records")
    public List<GameRecordDto> playerRecords(@PathVariable String playerId) {
        return service.findRecordsForPlayer(playerId).stream()
                .map(GameRecordDto::from)
                .toList();
    }

    @GetMapping("/games/{gameId}/record")
    public GameRecordDto gameRecord(@PathVariable String gameId) {
        return GameRecordDto.from(service.getRecordForGame(gameId));
    }

    @ExceptionHandler(GameRecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(GameRecordNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of("GAME_RECORD_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_RECORD_REQUEST", ex.getMessage()));
    }
}