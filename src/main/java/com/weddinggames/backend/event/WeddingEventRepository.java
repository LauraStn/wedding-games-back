package com.weddinggames.backend.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeddingEventRepository extends JpaRepository<WeddingEvent, UUID> {

    Optional<WeddingEvent> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
