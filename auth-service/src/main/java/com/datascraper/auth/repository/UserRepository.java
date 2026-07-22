package com.datascraper.auth.repository;

import com.datascraper.auth.domain.User;
import com.datascraper.auth.entity.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL-backed user store (Phase 13).
 */
@Repository
public class UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    public Optional<User> findByGoogleSubject(String googleSubject) {
        return userJpaRepository.findByGoogleSubject(googleSubject).map(this::toDomain);
    }

    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    public User save(User user) {
        UserEntity entity = userJpaRepository.findById(user.getId())
                .orElseGet(UserEntity::new);
        applyDomain(entity, user);
        return toDomain(userJpaRepository.save(entity));
    }

    private void applyDomain(UserEntity entity, User user) {
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setDisplayName(user.getDisplayName());
        entity.setPictureUrl(user.getPictureUrl());
        entity.setGoogleSubject(user.getGoogleSubject());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setLastLoginAt(user.getLastLoginAt());
    }

    private User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPictureUrl(),
                entity.getGoogleSubject(),
                entity.getCreatedAt(),
                entity.getLastLoginAt()
        );
    }
}
