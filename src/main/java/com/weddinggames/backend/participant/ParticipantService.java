package com.weddinggames.backend.participant;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingExclusionRepository;
import com.weddinggames.backend.participant.dto.ParticipantCreateRequest;
import com.weddinggames.backend.participant.dto.ParticipantUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final PairingExclusionRepository pairingExclusionRepository;

    public ParticipantService(
            ParticipantRepository participantRepository,
            WeddingEventRepository weddingEventRepository,
            PairingExclusionRepository pairingExclusionRepository) {
        this.participantRepository = participantRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.pairingExclusionRepository = pairingExclusionRepository;
    }

    @Transactional(readOnly = true)
    public List<Participant> listByEvent(UUID eventId) {
        return participantRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<Participant> search(
            UUID eventId, ParticipantStatus status, String tableLabel, ParticipantType participantType, String query) {
        return participantRepository.search(eventId, status, tableLabel, participantType, query);
    }

    @Transactional(readOnly = true)
    public Participant get(UUID id) {
        return participantRepository.findById(id).orElseThrow(() -> new NotFoundException("Participant introuvable."));
    }

    @Transactional
    public Participant create(UUID eventId, ParticipantCreateRequest request) {
        WeddingEvent event = weddingEventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        Participant participant = new Participant(
                event,
                request.firstName(),
                request.lastName(),
                request.displayName(),
                request.tableLabel(),
                request.participantType());
        participant.setGender(request.gender());
        return participantRepository.save(participant);
    }

    @Transactional
    public Participant update(UUID id, ParticipantUpdateRequest request) {
        Participant participant = get(id);
        participant.setFirstName(request.firstName());
        participant.setLastName(request.lastName());
        participant.setDisplayName(request.displayName());
        participant.setTableLabel(request.tableLabel());
        participant.setParticipantType(request.participantType());
        participant.setStatus(request.status());
        participant.setGender(request.gender());
        return participant;
    }

    @Transactional
    public void delete(UUID id) {
        if (!participantRepository.existsById(id)) {
            throw new NotFoundException("Participant introuvable.");
        }
        if (pairingExclusionRepository.existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
                ExclusionType.HARD, id, ExclusionType.HARD, id)) {
            throw new BusinessRuleViolationException(
                    "PARTICIPANT_HAS_HARD_EXCLUSION",
                    "Ce participant est implique dans une exclusion absolue (HARD) et ne peut pas etre supprime.");
        }
        participantRepository.deleteById(id);
    }

    @Transactional
    public Participant disable(UUID id) {
        Participant participant = get(id);
        participant.setStatus(ParticipantStatus.DISABLED);
        return participant;
    }

    @Transactional
    public Participant updateStatus(UUID id, ParticipantStatus status) {
        Participant participant = get(id);
        participant.setStatus(status);
        return participant;
    }

    @Transactional
    public Participant updateTable(UUID id, String tableLabel) {
        Participant participant = get(id);
        participant.setTableLabel(tableLabel);
        return participant;
    }
}
