package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerProfile;
import com.chesscard.shengji.service.PlayerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPlayerRepository implements PlayerRepository {
    private final PlayerProfileJpaRepository jpaRepository;

    public JpaPlayerRepository(PlayerProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlayerProfile save(PlayerProfile profile) {
        return jpaRepository.save(profile);
    }

    @Override
    public Optional<PlayerProfile> find(String id) {
        return jpaRepository.findById(id);
    }
}
