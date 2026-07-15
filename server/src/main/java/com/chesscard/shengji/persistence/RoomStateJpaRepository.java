package com.chesscard.shengji.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomStateJpaRepository extends JpaRepository<RoomStateEntity, String> {
    List<RoomStateEntity> findByPhaseOrderByUpdatedAtDesc(String phase);
}
