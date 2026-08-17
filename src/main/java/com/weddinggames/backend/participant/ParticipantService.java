package com.weddinggames.backend.participant;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
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

    public ParticipantService(
            ParticipantRepository participantRepository, WeddingEventRepository weddingEventRepository) {
        this.participantRepository = participantRepository;
        this.weddingEventRepository = weddingEventRepository;
    }

    @Transactional(readOnly = true)
    public List<Participant> listByEvent(UUID eventId) {
        return participantRepository.findByEventId(eventId);
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
        return participant;
    }

    @Transactional
    public void delete(UUID id) {
        if (!participantRepository.existsById(id)) {
            throw new NotFoundException("Participant introuvable.");
        }
        participantRepository.deleteById(id);
    }
}
