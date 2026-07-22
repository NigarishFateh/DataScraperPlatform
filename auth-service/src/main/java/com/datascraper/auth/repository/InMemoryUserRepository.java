package com.datascraper.auth.repository;

import com.datascraper.auth.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 4: in-memory user store.
 * Phase 13 replaces this with PostgreSQL without changing AuthService call sites (Repository Pattern).
 */
@Repository
public class InMemoryUserRepository {

    private final Map<UUID, User> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byGoogleSubject = new ConcurrentHashMap<>();
    private final Map<String, UUID> byEmail = new ConcurrentHashMap<>();

    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<User> findByGoogleSubject(String googleSubject) {
        UUID id = byGoogleSubject.get(googleSubject);
        return id == null ? Optional.empty() : findById(id);
    }

    public Optional<User> findByEmail(String email) {
        UUID id = byEmail.get(email.toLowerCase());
        return id == null ? Optional.empty() : findById(id);
    }

    public User save(User user) {
        byId.put(user.getId(), user);
        byGoogleSubject.put(user.getGoogleSubject(), user.getId());
        byEmail.put(user.getEmail().toLowerCase(), user.getId());
        return user;
    }
}
