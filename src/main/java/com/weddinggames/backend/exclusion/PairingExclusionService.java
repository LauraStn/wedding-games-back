package com.weddinggames.backend.exclusion;

import com.weddinggames.backend.common.audit.AuditAction;
import com.weddinggames.backend.common.audit.AuditLogService;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.ConflictException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.dto.PairingExclusionCreateRequest;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PairingExclusionService {

    private final PairingExclusionRepository pairingExclusionRepository;
    private final ParticipantRepository participantRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final AuditLogService auditLogService;

    public PairingExclusionService(
            PairingExclusionRepository pairingExclusionRepository,
            ParticipantRepository participantRepository,
            WeddingEventRepository weddingEventRepository,
            AuditLogService auditLogService) {
        this.pairingExclusionRepository = pairingExclusionRepository;
        this.participantRepository = participantRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PairingExclusion> listByEvent(UUID eventId) {
        return pairingExclusionRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public PairingExclusion get(UUID id) {
        return pairingExclusionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Exclusion introuvable."));
    }

    @Transactional
    public PairingExclusion create(UUID eventId, PairingExclusionCreateRequest request, UUID staffAccountId) {
        if (request.participantAId().equals(request.participantBId())) {
            throw new InvalidRequestException(
                    "SAME_PARTICIPANT", "Un participant ne peut pas etre exclu de lui-meme.");
        }
        WeddingEvent event = weddingEventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evenement introuvable."));

        UUID lowId = ExclusionPair.lower(request.participantAId(), request.participantBId());
        UUID highId = ExclusionPair.higher(request.participantAId(), request.participantBId());
        Participant low = participantRepository
                .findById(lowId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        Participant high = participantRepository
                .findById(highId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));

        if (pairingExclusionRepository.existsByEventIdAndParticipantAIdAndParticipantBId(eventId, lowId, highId)) {
            throw new ConflictException(
                    "EXCLUSION_ALREADY_EXISTS", "Une exclusion existe deja entre ces deux participants.");
        }

        PairingExclusion exclusion = new PairingExclusion(event, low, high, request.reason(), request.exclusionType());
        PairingExclusion saved = pairingExclusionRepository.save(exclusion);
        if (saved.getExclusionType() == ExclusionType.HARD) {
            auditLogService.record(
                    staffAccountId,
                    AuditAction.HARD_EXCLUSION_CREATED,
                    eventId,
                    saved.getId(),
                    low.getDisplayName() + " / " + high.getDisplayName());
        }
        return saved;
    }

    @Transactional
    public PairingExclusion updateReason(UUID id, String reason, UUID staffAccountId) {
        PairingExclusion exclusion = get(id);
        exclusion.setReason(reason);
        if (exclusion.getExclusionType() == ExclusionType.HARD) {
            auditLogService.record(
                    staffAccountId,
                    AuditAction.HARD_EXCLUSION_REASON_UPDATED,
                    exclusion.getEvent().getId(),
                    exclusion.getId(),
                    reason);
        }
        return exclusion;
    }

    @Transactional
    public void delete(UUID id) {
        PairingExclusion exclusion = get(id);
        if (exclusion.getExclusionType() == ExclusionType.HARD) {
            throw new BusinessRuleViolationException(
                    "HARD_EXCLUSION_IMMUTABLE",
                    "Une exclusion absolue (HARD) ne peut jamais etre supprimee via l'API.");
        }
        pairingExclusionRepository.delete(exclusion);
    }
}
