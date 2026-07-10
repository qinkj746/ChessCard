package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileJpaRepository extends JpaRepository<PlayerProfile, String> {
}
