package com.weddinggames.backend.invitation;

import com.weddinggames.backend.common.OpaqueTokenGenerator;
import com.weddinggames.backend.common.exception.InvalidInvitationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.invitation.dto.InvitationAdminResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ParticipantRepository participantRepository;
    private final InvitationProperties invitationProperties;
    private final Clock clock;

    public InvitationService(
            InvitationRepository invitationRepository,
            ParticipantRepository participantRepository,
            InvitationProperties invitationProperties,
            Clock clock) {
        this.invitationRepository = invitationRepository;
        this.participantRepository = participantRepository;
        this.invitationProperties = invitationProperties;
        this.clock = clock;
    }

    @Transactional
    public InvitationAdminResponse generateOrRegenerate(UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        return generateFor(participant, Instant.now(clock));
    }

    @Transactional
    public List<InvitationPrintCard> generateBatch(UUID eventId, List<UUID> participantIds) {
        List<Participant> participants = (participantIds == null || participantIds.isEmpty())
                ? participantRepository.findByEventId(eventId)
                : participantRepository.findByEventIdAndIdIn(eventId, participantIds);
        if (participants.isEmpty()) {
            throw new InvalidRequestException(
                    "INVITATION_BATCH_EMPTY", "Aucun participant trouve pour cette generation en lot.");
        }

        Instant now = Instant.now(clock);
        List<InvitationPrintCard> cards = new ArrayList<>();
        for (Participant participant : participants) {
            InvitationAdminResponse invitation = generateFor(participant, now);
            cards.add(new InvitationPrintCard(
                    participant.getDisplayName(), participant.getTableLabel(), invitation.invitationUrl()));
        }
        return cards;
    }

    private InvitationAdminResponse generateFor(Participant participant, Instant now) {
        List<Invitation> currentlyActive = invitationRepository.findByParticipantIdAndStatus(
                participant.getId(), InvitationStatus.ACTIVE);
        currentlyActive.forEach(invitation -> invitation.revoke(now));

        String rawToken = OpaqueTokenGenerator.generateRawToken();
        Invitation invitation = new Invitation(participant, OpaqueTokenGenerator.hash(rawToken));
        invitationRepository.save(invitation);

        String invitationUrl = invitationProperties.getBaseUrl() + "/" + rawToken;
        return new InvitationAdminResponse(
                invitation.getId(), participant.getId(), rawToken, invitationUrl, invitation.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Participant resolve(String rawToken) {
        return resolveActiveInvitation(rawToken).getParticipant();
    }

    @Transactional
    public Participant confirm(String rawToken) {
        Participant participant = resolveActiveInvitation(rawToken).getParticipant();
        participant.setStatus(ParticipantStatus.CONNECTED);
        return participant;
    }

    @Transactional(readOnly = true)
    public Invitation getCurrentInvitation(UUID participantId) {
        return invitationRepository
                .findByParticipantIdAndStatus(participantId, InvitationStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Aucune invitation active pour ce participant."));
    }

    private Invitation resolveActiveInvitation(String rawToken) {
        String hash = OpaqueTokenGenerator.hash(rawToken);
        Invitation invitation = invitationRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new InvalidInvitationException("Invitation invalide ou revoquee."));
        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new InvalidInvitationException("Invitation invalide ou revoquee.");
        }
        return invitation;
    }
}
