package com.weddinggames.backend.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSessionRepository extends JpaRepository<AppSession, UUID> {

    Optional<AppSession> findBySessionTokenHash(String sessionTokenHash);
}
