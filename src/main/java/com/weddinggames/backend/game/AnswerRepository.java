package com.weddinggames.backend.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByQuestionId(UUID questionId);

    Optional<Answer> findByQuestionIdAndTeamId(UUID questionId, UUID teamId);
}
