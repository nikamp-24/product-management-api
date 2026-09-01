package com.pooja.productmanagement.repository;

import com.pooja.productmanagement.entity.RefreshToken;
import com.pooja.productmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

}
