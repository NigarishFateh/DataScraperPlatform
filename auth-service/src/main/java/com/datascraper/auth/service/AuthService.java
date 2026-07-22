package com.datascraper.auth.service;

import com.datascraper.auth.config.AuthProperties;
import com.datascraper.auth.domain.GoogleProfile;
import com.datascraper.auth.domain.RefreshSession;
import com.datascraper.auth.domain.User;
import com.datascraper.auth.dto.AuthTokensResponse;
import com.datascraper.auth.dto.DevLoginRequest;
import com.datascraper.auth.dto.GoogleAuthRequest;
import com.datascraper.auth.exception.AuthException;
import com.datascraper.auth.repository.InMemoryRefreshSessionRepository;
import com.datascraper.auth.repository.InMemoryUserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final InMemoryUserRepository users;
    private final InMemoryRefreshSessionRepository refreshSessions;
    private final GoogleIdentityClient googleIdentityClient;
    private final JwtService jwtService;
    private final AuthProperties properties;

    public AuthService(
            InMemoryUserRepository users,
            InMemoryRefreshSessionRepository refreshSessions,
            GoogleIdentityClient googleIdentityClient,
            JwtService jwtService,
            AuthProperties properties
    ) {
        this.users = users;
        this.refreshSessions = refreshSessions;
        this.googleIdentityClient = googleIdentityClient;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    public AuthTokensResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleProfile profile;
        if (request.accessToken() != null && !request.accessToken().isBlank()) {
            profile = googleIdentityClient.verifyAccessToken(request.accessToken());
        } else {
            profile = googleIdentityClient.verifyIdToken(request.idToken());
        }
        User user = upsertGoogleUser(profile);
        return issueTokens(user);
    }

    public AuthTokensResponse loginDev(DevLoginRequest request) {
        if (!properties.getAuth().isDevLoginEnabled()) {
            throw new AuthException("Dev login is disabled");
        }
        String email = request.email().trim().toLowerCase();
        String name = request.displayName() == null || request.displayName().isBlank()
                ? email
                : request.displayName().trim();

        User user = users.findByEmail(email).orElseGet(() -> {
            Instant now = Instant.now();
            return users.save(new User(
                    UUID.randomUUID(),
                    email,
                    name,
                    null,
                    "dev:" + email,
                    now,
                    now
            ));
        });
        user.markLogin(Instant.now());
        return issueTokens(user);
    }

    public AuthTokensResponse refresh(String refreshToken) {
        RefreshSession session = refreshSessions.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        Instant now = Instant.now();
        if (session.isRevoked() || session.isExpired(now)) {
            refreshSessions.delete(refreshToken);
            throw new AuthException("Refresh token expired or revoked");
        }

        // Rotation: old refresh token dies, new one is born (stolen-token blast radius shrinks).
        session.revoke();
        refreshSessions.delete(refreshToken);

        User user = users.findById(session.getUserId())
                .orElseThrow(() -> new AuthException("User no longer exists"));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshSessions.findByToken(refreshToken).ifPresent(session -> {
            session.revoke();
            refreshSessions.delete(refreshToken);
        });
    }

    public AuthTokensResponse.UserResponse currentUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        return toUserResponse(user);
    }

    private User upsertGoogleUser(GoogleProfile profile) {
        Instant now = Instant.now();
        User user = users.findByGoogleSubject(profile.subject()).orElseGet(() ->
                users.findByEmail(profile.email().toLowerCase()).orElseGet(() ->
                        users.save(new User(
                                UUID.randomUUID(),
                                profile.email().toLowerCase(),
                                blankTo(profile.name(), profile.email()),
                                blankToNull(profile.pictureUrl()),
                                profile.subject(),
                                now,
                                now
                        ))
                )
        );
        user.markLogin(now);
        return user;
    }

    private AuthTokensResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        Instant refreshExpiry = Instant.now()
                .plus(properties.getJwt().getRefreshTokenTtlDays(), ChronoUnit.DAYS);
        refreshSessions.save(new RefreshSession(refreshToken, user.getId(), refreshExpiry));

        return AuthTokensResponse.bearer(
                accessToken,
                refreshToken,
                jwtService.accessTokenTtlSeconds(),
                toUserResponse(user)
        );
    }

    private static AuthTokensResponse.UserResponse toUserResponse(User user) {
        return new AuthTokensResponse.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPictureUrl()
        );
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
