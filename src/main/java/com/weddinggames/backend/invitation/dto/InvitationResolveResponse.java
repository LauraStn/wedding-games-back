package com.weddinggames.backend.invitation.dto;

import com.weddinggames.backend.participant.Participant;
import java.util.UUID;

/** Identity preview shown by the frontend so the guest can confirm "is this you?" before a session is created. */
public record InvitationResolveResponse(
        UUID participantId, String firstName, String displayName, String eventSlug, String eventTitle) {

    public static InvitationResolveResponse from(Participant participant) {
        return new InvitationResolveResponse(
                participant.getId(),
                participant.getFirstName(),
                participant.getDisplayName(),
                participant.getEvent().getSlug(),
                participant.getEvent().getTitle());
    }
}
