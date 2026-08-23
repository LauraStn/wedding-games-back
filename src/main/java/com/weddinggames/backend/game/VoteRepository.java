package com.weddinggames.backend.game;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByQuestionId(UUID questionId);

    boolean existsByQuestionIdAndVoterParticipantId(UUID questionId, UUID voterParticipantId);
}
