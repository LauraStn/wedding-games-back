package com.weddinggames.backend.participant.dto;

import java.util.List;

public record ParticipantImportConfirmResponse(List<ParticipantResponse> created, int createdCount) {

    public static ParticipantImportConfirmResponse from(List<ParticipantResponse> created) {
        return new ParticipantImportConfirmResponse(created, created.size());
    }
}
