package com.payflow.controller;

import com.payflow.dto.request.LoginRequest;
import com.payflow.dto.request.RegisterRequest;
import com.payflow.dto.response.ApiResponse;
import com.payflow.dto.response.AuthResponse;
import com.payflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Onboarding", description = "Endpoints for user registration and JWT authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register user and provision digital wallet", description = "Creates a new user profile and automatically assigns an active digital wallet with initial zero balance.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered and wallet initialized successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and issues a stateless HMAC-SHA256 JWT bearer token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}
