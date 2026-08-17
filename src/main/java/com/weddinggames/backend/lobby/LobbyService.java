package com.weddinggames.backend.lobby;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final LobbyParticipantRepository lobbyParticipantRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final ParticipantRepository participantRepository;
    private final Clock clock;

    public LobbyService(
            LobbyRepository lobbyRepository,
            LobbyParticipantRepository lobbyParticipantRepository,
            WeddingEventRepository weddingEventRepository,
            ParticipantRepository participantRepository,
            Clock clock) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    @Transactional
    public Lobby getOrCreate(UUID eventId) {
        return lobbyRepository.findByEventId(eventId).orElseGet(() -> {
            WeddingEvent event = weddingEventRepository
                    .findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Evenement introuvable."));
            return lobbyRepository.save(new Lobby(event));
        });
    }

    @Transactional
    public Lobby open(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.open(Instant.now(clock));
        return lobby;
    }

    @Transactional
    public Lobby close(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.close(Instant.now(clock));
        return lobby;
    }

    @Transactional
    public Lobby lock(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.lock();
        return lobby;
    }

    @Transactional(readOnly = true)
    public List<LobbyParticipant> listParticipants(UUID eventId) {
        Lobby lobby = lobbyRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun salon pour cet evenement."));
        return lobbyParticipantRepository.findByLobbyId(lobby.getId());
    }

    @Transactional
    public LobbyParticipant heartbeat(UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        Lobby lobby = getOrCreate(participant.getEvent().getId());
        Instant now = Instant.now(clock);
        return lobbyParticipantRepository
                .findByLobbyIdAndParticipantId(lobby.getId(), participantId)
                .map(existing -> {
                    existing.heartbeat(now);
                    return existing;
                })
                .orElseGet(() -> lobbyParticipantRepository.save(new LobbyParticipant(lobby, participant, now)));
    }

    @Transactional
    public LobbyParticipant markLate(UUID eventId, UUID participantId) {
        LobbyParticipant lobbyParticipant = getOrCreateEntry(eventId, participantId);
        lobbyParticipant.markLate();
        return lobbyParticipant;
    }

    @Transactional
    public LobbyParticipant admit(UUID eventId, UUID participantId) {
        LobbyParticipant lobbyParticipant = getOrCreateEntry(eventId, participantId);
        lobbyParticipant.admit(Instant.now(clock));
        return lobbyParticipant;
    }

    private LobbyParticipant getOrCreateEntry(UUID eventId, UUID participantId) {
        Lobby lobby = getOrCreate(eventId);
        return lobbyParticipantRepository
                .findByLobbyIdAndParticipantId(lobby.getId(), participantId)
                .orElseGet(() -> {
                    Participant participant = participantRepository
                            .findById(participantId)
                            .orElseThrow(() -> new NotFoundException("Participant introuvable."));
                    return lobbyParticipantRepository.save(
                            new LobbyParticipant(lobby, participant, Instant.now(clock)));
                });
    }
}
