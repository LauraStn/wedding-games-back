package com.weddinggames.backend.session.dto;

import com.weddinggames.backend.security.ActorType;
import com.weddinggames.backend.security.Role;
import com.weddinggames.backend.participant.dto.ParticipantSessionResponse;
import com.weddinggames.backend.staff.dto.StaffAccountResponse;

public record SessionMeResponse(
        ActorType actorType, Role role, ParticipantSessionResponse participant, StaffAccountResponse staff) {

    public static SessionMeResponse ofParticipant(ParticipantSessionResponse participant) {
        return new SessionMeResponse(ActorType.PARTICIPANT, Role.PARTICIPANT, participant, null);
    }

    public static SessionMeResponse ofStaff(StaffAccountResponse staff, Role role) {
        return new SessionMeResponse(ActorType.STAFF, role, null, staff);
    }
}
