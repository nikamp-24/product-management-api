package com.pooja.productmanagement.service;

import com.pooja.productmanagement.entity.RefreshToken;
import com.pooja.productmanagement.entity.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyExpiration(RefreshToken token);

    void deleteByUser(User user);

}
