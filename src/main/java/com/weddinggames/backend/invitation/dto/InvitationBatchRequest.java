package com.weddinggames.backend.invitation.dto;

import java.util.List;
import java.util.UUID;

/** {@code participantIds} null or empty means "every participant of the event". */
public record InvitationBatchRequest(List<UUID> participantIds) {}
