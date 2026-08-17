package com.weddinggames.backend.exclusion;

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

    public PairingExclusionService(
            PairingExclusionRepository pairingExclusionRepository,
            ParticipantRepository participantRepository,
            WeddingEventRepository weddingEventRepository) {
        this.pairingExclusionRepository = pairingExclusionRepository;
        this.participantRepository = participantRepository;
        this.weddingEventRepository = weddingEventRepository;
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
    public PairingExclusion create(UUID eventId, PairingExclusionCreateRequest request) {
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
        return pairingExclusionRepository.save(exclusion);
    }

    @Transactional
    public PairingExclusion updateReason(UUID id, String reason) {
        PairingExclusion exclusion = get(id);
        exclusion.setReason(reason);
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
