package com.pooja.productmanagement.controller;

import com.pooja.productmanagement.dto.ApiResponse;
import com.pooja.productmanagement.dto.JwtResponse;
import com.pooja.productmanagement.dto.LoginRequest;
import com.pooja.productmanagement.dto.RefreshTokenRequest;
import com.pooja.productmanagement.dto.RefreshTokenResponse;
import com.pooja.productmanagement.dto.RegisterRequest;
import com.pooja.productmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user authentication and registration endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<String> response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

}
