package com.weddinggames.backend.whosaidit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhoSaidItQuestionRepository extends JpaRepository<WhoSaidItQuestion, UUID> {

    List<WhoSaidItQuestion> findByAuthorId(UUID authorId);

    Optional<WhoSaidItQuestion> findByIdAndAuthorId(UUID id, UUID authorId);

    long countByAuthorId(UUID authorId);

    List<WhoSaidItQuestion> findByEventId(UUID eventId);

    List<WhoSaidItQuestion> findByEventIdAndStatus(UUID eventId, WhoSaidItQuestionStatus status);
}
