package com.chesscard.shengji.service;

import com.chesscard.shengji.persistence.GameSessionJpaRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class GamePersistenceTest {
    @Autowired
    private GameService gameService;

    @Autowired
    private GameSessionJpaRepository jpaRepository;

    @Test
    void createGamePersistsSnapshotAndCanLoadItBack() {
        String id = gameService.createGame().getId();

        assertThat(jpaRepository.findById(id)).isPresent();
        assertThat(gameService.getGame(id).getHands().get(com.chesscard.shengji.domain.PlayerSeat.SOUTH)).hasSize(25);
    }
}
