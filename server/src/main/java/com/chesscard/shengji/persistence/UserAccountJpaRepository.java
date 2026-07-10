package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountJpaRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findBySessionToken(String sessionToken);
}