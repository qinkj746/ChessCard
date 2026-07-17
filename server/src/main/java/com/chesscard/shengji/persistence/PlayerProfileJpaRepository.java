package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PlayerProfileJpaRepository extends JpaRepository<PlayerProfile, String> {
    List<PlayerProfile> findByLastSeenAtGreaterThanEqualOrderByLastSeenAtDesc(Instant cutoff);
}
