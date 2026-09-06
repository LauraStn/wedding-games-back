package com.weddinggames.backend.jury;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuryDecisionRepository extends JpaRepository<JuryDecision, UUID> {

    Optional<JuryDecision> findByQuestionId(UUID questionId);
}
