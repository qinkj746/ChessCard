package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {
    UserAccount save(UserAccount account);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findBySessionToken(String sessionToken);
}