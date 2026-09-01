package com.productmanagement.service.impl;

import com.productmanagement.dto.ApiResponse;
import com.productmanagement.dto.JwtResponse;
import com.productmanagement.dto.LoginRequest;
import com.productmanagement.dto.RefreshTokenRequest;
import com.productmanagement.dto.RefreshTokenResponse;
import com.productmanagement.dto.RegisterRequest;
import com.productmanagement.entity.RefreshToken;
import com.productmanagement.entity.Role;
import com.productmanagement.entity.RoleName;
import com.productmanagement.entity.User;
import com.productmanagement.exception.BadRequestException;
import com.productmanagement.exception.DuplicateResourceException;
import com.productmanagement.exception.ResourceNotFoundException;
import com.productmanagement.exception.TokenRefreshException;
import com.productmanagement.exception.UnauthorizedException;
import com.productmanagement.repository.RoleRepository;
import com.productmanagement.repository.UserRepository;
import com.productmanagement.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;
    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 900000L);

        userRole = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
        user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("encoded_pass")
                .role(userRole)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("mock-refresh-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john")
                .email("john@example.com")
                .password("password123")
                .role("ROLE_USER")
                .build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");

        ApiResponse<String> response = authService.register(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("User registered successfully");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ThrowsDuplicateResourceException_WhenUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder().username("john").email("john@example.com").build();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username 'john' is already taken");
    }

    @Test
    void register_ThrowsDuplicateResourceException_WhenEmailTaken() {
        RegisterRequest request = RegisterRequest.builder().username("john").email("john@example.com").build();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email 'john@example.com' is already in use");
    }

    @Test
    void register_ThrowsBadRequestException_WhenRoleInvalid() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john")
                .email("john@example.com")
                .role("ROLE_SUPERUSER")
                .build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role specified");
    }

    @Test
    void register_ThrowsResourceNotFoundException_WhenRoleNotFoundInDb() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john")
                .email("john@example.com")
                .role("ROLE_USER")
                .build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void login_Success() {
        LoginRequest request = LoginRequest.builder().username("john").password("password123").build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        JwtResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_ThrowsUnauthorizedException_WhenUserNotFound() {
        LoginRequest request = LoginRequest.builder().username("nonexistent").password("pass").build();
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_ThrowsUnauthorizedException_WhenPasswordMismatch() {
        LoginRequest request = LoginRequest.builder().username("john").password("wrong_password").build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_pass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("mock-refresh-token").build();

        when(refreshTokenService.findByToken("mock-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        RefreshTokenResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
    }

    @Test
    void refreshToken_ThrowsTokenRefreshException_WhenTokenNotFound() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("invalid-token").build();

        when(refreshTokenService.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Refresh token was expired or not found");
    }

}
