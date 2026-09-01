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
import com.productmanagement.service.AuthService;
import com.productmanagement.service.RefreshTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:900000}")
    private Long jwtExpirationMs;

    @Override
    @Transactional
    public ApiResponse<String> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        RoleName resolvedRole = RoleName.ROLE_USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                resolvedRole = RoleName.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid role specified: " + request.getRole());
            }
        }
        final RoleName roleName = resolvedRole;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        return ApiResponse.success("User registered successfully");
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String roleName = user.getRole().getName().name();
        String accessToken = generateAccessToken(user.getUsername(), roleName);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(roleName)
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new TokenRefreshException("Refresh token was expired or not found"));

        User user = token.getUser();
        String roleName = user.getRole().getName().name();
        String newAccessToken = generateAccessToken(user.getUsername(), roleName);
        RefreshToken rotatedToken = refreshTokenService.createRefreshToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotatedToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    private String generateAccessToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
