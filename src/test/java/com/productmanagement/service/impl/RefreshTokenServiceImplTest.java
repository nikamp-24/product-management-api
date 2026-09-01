package com.productmanagement.service.impl;

import com.productmanagement.entity.RefreshToken;
import com.productmanagement.entity.User;
import com.productmanagement.exception.TokenRefreshException;
import com.productmanagement.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);

        user = User.builder().id(1L).username("testuser").build();
        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("sample-uuid-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void createRefreshToken_Success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("sample-uuid-token");
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findByToken_Success() {
        when(refreshTokenRepository.findByToken("sample-uuid-token")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("sample-uuid-token");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("sample-uuid-token");
    }

    @Test
    void findByToken_NotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken("invalid-token");

        assertThat(result).isEmpty();
    }

    @Test
    void verifyExpiration_ValidToken() {
        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("sample-uuid-token");
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_ExpiredToken_ThrowsTokenRefreshException() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(2L)
                .token("expired-token")
                .user(user)
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Refresh token was expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void deleteByUser_Success() {
        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }

}
