package com.weddinggames.backend.character;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, UUID> {

    List<GameCharacter> findByEventId(UUID eventId);

    boolean existsByEventIdAndName(UUID eventId, String name);
}
