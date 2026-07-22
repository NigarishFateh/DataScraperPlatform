package com.datascraper.auth.repository;

import com.datascraper.auth.domain.RefreshSession;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRefreshSessionRepository {

    private final Map<String, RefreshSession> byToken = new ConcurrentHashMap<>();

    public RefreshSession save(RefreshSession session) {
        byToken.put(session.getToken(), session);
        return session;
    }

    public Optional<RefreshSession> findByToken(String token) {
        return Optional.ofNullable(byToken.get(token));
    }

    public void delete(String token) {
        byToken.remove(token);
    }
}
