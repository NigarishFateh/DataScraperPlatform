package com.datascraper.auth.repository;

import com.datascraper.auth.domain.RefreshSession;
import com.datascraper.auth.entity.RefreshSessionEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostgreSQL-backed refresh token store (Phase 13).
 */
@Repository
public class RefreshSessionRepository {

    private final RefreshSessionJpaRepository refreshSessionJpaRepository;

    public RefreshSessionRepository(RefreshSessionJpaRepository refreshSessionJpaRepository) {
        this.refreshSessionJpaRepository = refreshSessionJpaRepository;
    }

    public RefreshSession save(RefreshSession session) {
        RefreshSessionEntity entity = refreshSessionJpaRepository.findById(session.getToken())
                .orElseGet(RefreshSessionEntity::new);
        applyDomain(entity, session);
        return toDomain(refreshSessionJpaRepository.save(entity));
    }

    public Optional<RefreshSession> findByToken(String token) {
        return refreshSessionJpaRepository.findByToken(token).map(this::toDomain);
    }

    public void delete(String token) {
        refreshSessionJpaRepository.deleteByToken(token);
    }

    private void applyDomain(RefreshSessionEntity entity, RefreshSession session) {
        entity.setToken(session.getToken());
        entity.setUserId(session.getUserId());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setRevoked(session.isRevoked());
    }

    private RefreshSession toDomain(RefreshSessionEntity entity) {
        RefreshSession session = new RefreshSession(entity.getToken(), entity.getUserId(), entity.getExpiresAt());
        if (entity.isRevoked()) {
            session.revoke();
        }
        return session;
    }
}
