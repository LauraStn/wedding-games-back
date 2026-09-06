package com.weddinggames.backend.blindtest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlindTestFormatRepository extends JpaRepository<BlindTestFormat, UUID> {

    Optional<BlindTestFormat> findByGameId(UUID gameId);
}
