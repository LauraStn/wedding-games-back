package com.weddinggames.backend.lobby;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LobbyRepository extends JpaRepository<Lobby, UUID> {

    Optional<Lobby> findByEventId(UUID eventId);
}
