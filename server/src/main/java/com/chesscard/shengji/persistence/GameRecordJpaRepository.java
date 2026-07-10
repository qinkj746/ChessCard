package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRecordJpaRepository extends JpaRepository<GameRecord, String> {
    Optional<GameRecord> findByGameId(String gameId);

    @Query("select distinct record from GameRecord record join record.players players where value(players) = :playerId")
    List<GameRecord> findByPlayerId(@Param("playerId") String playerId);
}