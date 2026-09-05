package com.weddinggames.backend.lobby;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.lobby.dto.LobbyParticipantResponse;
import com.weddinggames.backend.lobby.dto.LobbyParticipantStatusResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.security.AppSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final LobbyParticipantRepository lobbyParticipantRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final ParticipantRepository participantRepository;
    private final AppSessionRepository appSessionRepository;
    private final Clock clock;

    public LobbyService(
            LobbyRepository lobbyRepository,
            LobbyParticipantRepository lobbyParticipantRepository,
            WeddingEventRepository weddingEventRepository,
            ParticipantRepository participantRepository,
            AppSessionRepository appSessionRepository,
            Clock clock) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.participantRepository = participantRepository;
        this.appSessionRepository = appSessionRepository;
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

    @Transactional
    public Lobby start(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.start();
        return lobby;
    }

    @Transactional
    public Lobby pause(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.pause();
        return lobby;
    }

    @Transactional
    public Lobby resume(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.resume();
        return lobby;
    }

    @Transactional
    public Lobby finish(UUID eventId) {
        Lobby lobby = getOrCreate(eventId);
        lobby.finish();
        return lobby;
    }

    @Transactional(readOnly = true)
    public List<LobbyParticipant> listParticipants(UUID eventId) {
        Lobby lobby = lobbyRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun salon pour cet evenement."));
        return lobbyParticipantRepository.findByLobbyId(lobby.getId());
    }

    /**
     * Staff-facing view enriched with two signals the intervenant needs before launching:
     * participants whose name collides with another guest of the same event (likely a
     * duplicate import), and participants with more than one currently valid session (their
     * QR/fallback code was possibly used from more than one device).
     */
    @Transactional(readOnly = true)
    public List<LobbyParticipantResponse> listParticipantViews(UUID eventId) {
        List<LobbyParticipant> lobbyParticipants = listParticipants(eventId);
        Set<UUID> duplicateParticipantIds = findDuplicateParticipantIds(eventId);
        Instant now = Instant.now(clock);
        return lobbyParticipants.stream()
                .map(lobbyParticipant -> {
                    UUID participantId = lobbyParticipant.getParticipant().getId();
                    boolean possibleDuplicate = duplicateParticipantIds.contains(participantId);
                    boolean possibleQrReuse = countValidSessions(participantId, now) > 1;
                    return LobbyParticipantResponse.from(lobbyParticipant, possibleDuplicate, possibleQrReuse);
                })
                .toList();
    }

    private Set<UUID> findDuplicateParticipantIds(UUID eventId) {
        Map<String, List<Participant>> byNameKey = participantRepository.findByEventId(eventId).stream()
                .collect(Collectors.groupingBy(
                        p -> normalize(p.getFirstName()) + "|" + normalize(p.getLastName())));
        return byNameKey.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .map(Participant::getId)
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private long countValidSessions(UUID participantId, Instant now) {
        return appSessionRepository.findByParticipantIdAndRevokedAtIsNull(participantId).stream()
                .filter(session -> session.getExpiresAt().isAfter(now))
                .count();
    }

    /** Wraps a single, just-updated entry with the same duplicate/QR-reuse signals as {@link #listParticipantViews}. */
    @Transactional(readOnly = true)
    public LobbyParticipantResponse toView(UUID eventId, LobbyParticipant lobbyParticipant) {
        UUID participantId = lobbyParticipant.getParticipant().getId();
        boolean possibleDuplicate = findDuplicateParticipantIds(eventId).contains(participantId);
        boolean possibleQrReuse = countValidSessions(participantId, Instant.now(clock)) > 1;
        return LobbyParticipantResponse.from(lobbyParticipant, possibleDuplicate, possibleQrReuse);
    }

    @Transactional
    public LobbyParticipantStatusResponse getStatusForParticipant(UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        Lobby lobby = getOrCreate(participant.getEvent().getId());
        long presentCount =
                lobbyParticipantRepository.countByLobbyIdAndConnectionStatus(lobby.getId(), LobbyConnectionStatus.CONNECTED);
        return new LobbyParticipantStatusResponse(
                lobby.getStatus(), presentCount, participant.getEvent().getWelcomeMessage());
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
    public LobbyParticipant markReady(UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        Lobby lobby = getOrCreate(participant.getEvent().getId());
        Instant now = Instant.now(clock);
        return lobbyParticipantRepository
                .findByLobbyIdAndParticipantId(lobby.getId(), participantId)
                .map(existing -> {
                    existing.markReady(now);
                    return existing;
                })
                .orElseGet(() -> {
                    LobbyParticipant created = new LobbyParticipant(lobby, participant, now);
                    created.markReady(now);
                    return lobbyParticipantRepository.save(created);
                });
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
