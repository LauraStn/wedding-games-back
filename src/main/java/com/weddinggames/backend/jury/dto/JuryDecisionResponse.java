package com.weddinggames.backend.jury.dto;

import com.weddinggames.backend.jury.JuryDecision;
import com.weddinggames.backend.jury.JuryDecisionStatus;
import java.util.UUID;

public record JuryDecisionResponse(
        UUID questionId, UUID chosenAnswerId, UUID chosenTeamId, JuryDecisionStatus status, boolean revealed) {

    public static JuryDecisionResponse from(JuryDecision decision) {
        var answer = decision.getChosenAnswer();
        return new JuryDecisionResponse(
                decision.getQuestion().getId(),
                answer != null ? answer.getId() : null,
                answer != null ? answer.getTeam().getId() : null,
                decision.getStatus(),
                decision.isRevealed());
    }
}
