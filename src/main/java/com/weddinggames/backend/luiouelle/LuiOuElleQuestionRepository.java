package com.weddinggames.backend.luiouelle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LuiOuElleQuestionRepository extends JpaRepository<LuiOuElleQuestion, UUID> {

    List<LuiOuElleQuestion> findByAuthorId(UUID authorId);

    Optional<LuiOuElleQuestion> findByIdAndAuthorId(UUID id, UUID authorId);

    long countByAuthorId(UUID authorId);

    List<LuiOuElleQuestion> findByEventId(UUID eventId);

    List<LuiOuElleQuestion> findByEventIdAndStatus(UUID eventId, LuiOuElleQuestionStatus status);
}
