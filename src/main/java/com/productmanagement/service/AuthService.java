package com.productmanagement.service;

import com.productmanagement.dto.ApiResponse;
import com.productmanagement.dto.JwtResponse;
import com.productmanagement.dto.LoginRequest;
import com.productmanagement.dto.RefreshTokenRequest;
import com.productmanagement.dto.RefreshTokenResponse;
import com.productmanagement.dto.RegisterRequest;

public interface AuthService {

    ApiResponse<String> register(RegisterRequest request);

    JwtResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

}
