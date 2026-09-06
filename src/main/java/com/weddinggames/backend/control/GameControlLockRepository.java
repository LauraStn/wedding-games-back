package com.weddinggames.backend.control;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameControlLockRepository extends JpaRepository<GameControlLock, UUID> {

    Optional<GameControlLock> findByGameId(UUID gameId);
}
