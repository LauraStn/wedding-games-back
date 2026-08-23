package com.weddinggames.backend.game;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepository extends JpaRepository<Score, UUID> {

    List<Score> findByEventId(UUID eventId);

    List<Score> findByTeamId(UUID teamId);
}
