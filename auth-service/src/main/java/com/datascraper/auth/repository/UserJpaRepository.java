package com.datascraper.auth.repository;

import com.datascraper.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByGoogleSubject(String googleSubject);

    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
