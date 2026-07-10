package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CardDto;
import com.chesscard.shengji.api.dto.DeclareRequest;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.GameStateDto;
import com.chesscard.shengji.api.dto.KittyRequest;
import com.chesscard.shengji.api.dto.PlayRequest;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {
    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    @PostMapping
    public GameStateDto create() {
        return GameStateDto.from(service.createGame());
    }

    @GetMapping("/{id}")
    public GameStateDto get(@PathVariable String id) {
        return GameStateDto.from(service.getGame(idOrThrow(id)));
    }

    @PostMapping("/{id}/declare")
    public GameStateDto declare(@PathVariable String id, @RequestBody DeclareRequest request) {
        requestOrThrow(request);
        return GameStateDto.from(service.declare(idOrThrow(id), suitOrThrow(request.suit()), request.playerId()));
    }

    @PostMapping("/{id}/kitty")
    public GameStateDto kitty(@PathVariable String id, @RequestBody KittyRequest request) {
        requestOrThrow(request);
        return GameStateDto.from(service.setKitty(idOrThrow(id), cardsOrThrow(request.cards()).stream().map(CardDto::toCard).toList(), request.playerId()));
    }

    @PostMapping("/{id}/play")
    public GameStateDto play(@PathVariable String id, @RequestBody PlayRequest request) {
        requestOrThrow(request);
        return GameStateDto.from(service.play(idOrThrow(id), seatOrThrow(request.seat()), cardsOrThrow(request.cards()).stream().map(CardDto::toCard).toList(), request.playerId()));
    }

    @PostMapping("/{id}/ai/step")
    public GameStateDto aiStep(@PathVariable String id) {
        return GameStateDto.from(service.aiStep(idOrThrow(id)));
    }

    @PostMapping("/{id}/next")
    public GameStateDto next(@PathVariable String id) {
        return GameStateDto.from(service.createNextGame(idOrThrow(id)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_OPERATION", ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(GameNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of("GAME_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(PermissionDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of("PERMISSION_DENIED", ex.getMessage()));
    }

    private List<CardDto> cardsOrThrow(List<CardDto> cards) {
        if (cards == null) {
            throw new IllegalArgumentException("cards 不能为空");
        }
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("cards 不能为空");
        }
        if (cards.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("cards 不能为空");
        }
        if (cards.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("card rank 不能为空");
        }
        if (cards.stream().anyMatch(card -> !isJokerRank(card.rank()) && card.suit() == null)) {
            throw new IllegalArgumentException("card suit 不能为空");
        }
        if (cards.stream().anyMatch(card -> isJokerRank(card.rank()) && card.suit() != null)) {
            throw new IllegalArgumentException("joker suit 必须为空");
        }
        if (cards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("deckIndex 必须为 0 或 1");
        }
        if (cards.stream().distinct().count() != cards.size()) {
            throw new IllegalArgumentException("cards 不能包含重复牌");
        }
        return cards;
    }

    private String idOrThrow(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("game id 不能为空");
        }
        return id;
    }

    private boolean isJokerRank(Rank rank) {
        return rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER;
    }

    private Suit suitOrThrow(Suit suit) {
        if (suit == null) {
            throw new IllegalArgumentException("suit 不能为空");
        }
        return suit;
    }

    private PlayerSeat seatOrThrow(PlayerSeat seat) {
        if (seat == null) {
            throw new IllegalArgumentException("seat 不能为空");
        }
        return seat;
    }

    private void requestOrThrow(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
    }
}
