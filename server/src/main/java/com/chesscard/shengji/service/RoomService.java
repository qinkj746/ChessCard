package com.chesscard.shengji.service;

import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RoomService {
    private final RoomRepository repository;
    private final GameService gameService;
    private final RoomEventPublisher eventPublisher;

    public RoomService(RoomRepository repository, GameService gameService) {
        this(repository, gameService, RoomEventPublisher.noop());
    }

    @Autowired
    public RoomService(RoomRepository repository, GameService gameService, RoomEventPublisher eventPublisher) {
        this.repository = repository;
        this.gameService = gameService;
        this.eventPublisher = eventPublisher;
    }

    public RoomState createRoom(String ownerPlayerId) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            throw new IllegalArgumentException("ownerPlayerId 不能为空");
        }
        RoomState room = RoomState.create(ownerPlayerId);
        return saveAndPublish(room);
    }

    public RoomState getRoom(String roomId) {
        return repository.find(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在: " + roomId));
    }

    public RoomState joinSeat(String roomId, String playerId, PlayerSeat seat) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        if (seat == null) {
            throw new IllegalArgumentException("seat 不能为空");
        }
        RoomState room = getRoom(roomId);
        if (room.getPhase() != RoomPhase.WAITING) {
            throw new IllegalArgumentException("房间不在等待状态，无法入座");
        }
        if (room.getSeats().containsKey(seat)) {
            throw new IllegalArgumentException("座位已被占用: " + seat.name());
        }
        if (room.getSeats().values().stream().anyMatch(s -> s.getPlayerId().equals(playerId))) {
            throw new IllegalArgumentException("该玩家已入座其他座位");
        }
        room.getSeats().put(seat, new RoomSeat(seat, playerId, Instant.now()));
        room.touch();
        return saveAndPublish(room);
    }

    public RoomState leaveSeat(String roomId, String playerId, PlayerSeat seat) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        if (seat == null) {
            throw new IllegalArgumentException("seat 不能为空");
        }
        RoomState room = getRoom(roomId);
        if (room.getPhase() != RoomPhase.WAITING) {
            throw new IllegalArgumentException("房间不在等待状态，无法离座");
        }
        RoomSeat existing = room.getSeats().get(seat);
        if (existing == null) {
            throw new IllegalArgumentException("该座位无人占用");
        }
        if (!existing.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("只能离开自己占用的座位");
        }
        room.getSeats().remove(seat);
        room.touch();
        return saveAndPublish(room);
    }

    public GameState startGame(String roomId, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        RoomState room = getRoom(roomId);
        if (room.getPhase() != RoomPhase.WAITING) {
            throw new IllegalArgumentException("房间不在等待状态，无法开局");
        }
        if (!room.getOwnerPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("只有房主才能开局");
        }
        GameState game = gameService.createGameForRoom(roomId, room.getSeats());
        room.setGameId(game.getId());
        room.setPhase(RoomPhase.PLAYING);
        room.touch();
        saveAndPublish(room);
        return game;
    }

    private RoomState saveAndPublish(RoomState room) {
        RoomState saved = repository.save(room);
        eventPublisher.publish(RoomEvent.roomUpdated(saved));
        return saved;
    }
}
