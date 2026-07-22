package com.datascraper.auth.controller;

import com.datascraper.auth.dto.AuthTokensResponse;
import com.datascraper.auth.dto.DevLoginRequest;
import com.datascraper.auth.dto.GoogleAuthRequest;
import com.datascraper.auth.dto.RefreshTokenRequest;
import com.datascraper.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public AuthTokensResponse google(@Valid @RequestBody GoogleAuthRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/dev-login")
    public AuthTokensResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        return authService.loginDev(request);
    }

    @PostMapping("/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    @GetMapping("/me")
    public AuthTokensResponse.UserResponse me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return authService.currentUser(userId);
    }
}
