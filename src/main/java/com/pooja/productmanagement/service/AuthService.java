package com.pooja.productmanagement.service;

import com.pooja.productmanagement.dto.ApiResponse;
import com.pooja.productmanagement.dto.JwtResponse;
import com.pooja.productmanagement.dto.LoginRequest;
import com.pooja.productmanagement.dto.RefreshTokenRequest;
import com.pooja.productmanagement.dto.RefreshTokenResponse;
import com.pooja.productmanagement.dto.RegisterRequest;

/**
 * Service interface for authentication and token refresh operations.
 */
public interface AuthService {

    ApiResponse<String> register(RegisterRequest request);

    JwtResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

}
