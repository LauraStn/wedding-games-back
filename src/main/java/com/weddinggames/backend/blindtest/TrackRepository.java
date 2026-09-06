package com.weddinggames.backend.blindtest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, UUID> {

    List<Track> findByGameIdOrderBySequence(UUID gameId);
}
