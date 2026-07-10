package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.service.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaGameRepository implements GameRepository {
    private final GameSessionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public JpaGameRepository(GameSessionJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public GameState save(GameState game) {
        GameSessionEntity entity = new GameSessionEntity();
        entity.setId(game.getId());
        entity.setPhase(game.getPhase().name());
        entity.setLevelRank(game.getLevelRank().name());
        entity.setTrumpSuit(game.getTrumpSuit() == null ? null : game.getTrumpSuit().name());
        entity.setBanker(game.getBanker() == null ? null : game.getBanker().name());
        entity.setCurrentTurn(game.getCurrentTurn().name());
        entity.setAttackerScore(game.getAttackerScore());
        entity.setSnapshotJson(toJson(game));
        entity.setUpdatedAt(Instant.now());
        jpaRepository.save(entity);
        return game;
    }

    @Override
    public Optional<GameState> find(String id) {
        return jpaRepository.findById(id).map(entity -> fromJson(entity.getSnapshotJson()));
    }

    private String toJson(GameState game) {
        try {
            return objectMapper.writeValueAsString(game);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("牌局快照序列化失败", e);
        }
    }

    private GameState fromJson(String json) {
        try {
            return objectMapper.readValue(json, GameState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("牌局快照反序列化失败", e);
        }
    }
}
