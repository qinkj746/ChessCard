package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.UserAccount;
import com.chesscard.shengji.service.UserAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {
    private final UserAccountJpaRepository jpaRepository;

    public JpaUserAccountRepository(UserAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserAccount save(UserAccount account) {
        return jpaRepository.save(account);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jpaRepository.findById(username);
    }

    @Override
    public Optional<UserAccount> findBySessionToken(String sessionToken) {
        return jpaRepository.findBySessionToken(sessionToken);
    }
}