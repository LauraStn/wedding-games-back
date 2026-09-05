package com.weddinggames.backend.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.lobby.dto.LobbyParticipantResponse;
import com.weddinggames.backend.lobby.dto.LobbyParticipantStatusResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import com.weddinggames.backend.security.ActorType;
import com.weddinggames.backend.security.AppSession;
import com.weddinggames.backend.security.AppSessionRepository;
import com.weddinggames.backend.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the participant-facing lobby status aggregate. */
class LobbyServiceTest {

    private LobbyRepository lobbyRepository;
    private LobbyParticipantRepository lobbyParticipantRepository;
    private ParticipantRepository participantRepository;
    private AppSessionRepository appSessionRepository;
    private LobbyService service;
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        lobbyRepository = mock(LobbyRepository.class);
        lobbyParticipantRepository = mock(LobbyParticipantRepository.class);
        WeddingEventRepository weddingEventRepository = mock(WeddingEventRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        appSessionRepository = mock(AppSessionRepository.class);
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        service = new LobbyService(
                lobbyRepository,
                lobbyParticipantRepository,
                weddingEventRepository,
                participantRepository,
                appSessionRepository,
                clock);
    }

    @Test
    void aggregatesLobbyStatusPresentCountAndWelcomeMessageForAParticipant() {
        UUID participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        when(event.getId()).thenReturn(eventId);
        when(event.getWelcomeMessage()).thenReturn("Bienvenue !");
        Participant participant =
                new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        Lobby lobby = new Lobby(event);
        lobby.open(Instant.now());
        lobby.lock();
        lobby.start();
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        when(lobbyParticipantRepository.countByLobbyIdAndConnectionStatus(any(), any())).thenReturn(7L);

        LobbyParticipantStatusResponse status = service.getStatusForParticipant(participantId);

        assertThat(status.status()).isEqualTo(LobbyStatus.ACTIVE);
        assertThat(status.presentCount()).isEqualTo(7L);
        assertThat(status.welcomeMessage()).isEqualTo("Bienvenue !");
    }

    @Test
    void flagsParticipantsSharingTheSameNormalizedNameAsPossibleDuplicates() {
        // Mocked with distinct explicit ids: plain `new Participant(...)` all share a null id
        // (Hibernate only assigns one on persist), which would make every unpersisted participant
        // in this test collide as "the same" duplicate-set member.
        Participant alice = mock(Participant.class);
        when(alice.getId()).thenReturn(UUID.randomUUID());
        when(alice.getFirstName()).thenReturn("Alice");
        when(alice.getLastName()).thenReturn("Wonderland");
        when(alice.getDisplayName()).thenReturn("Alice W.");

        Participant aliceAgain = mock(Participant.class);
        when(aliceAgain.getId()).thenReturn(UUID.randomUUID());
        when(aliceAgain.getFirstName()).thenReturn(" alice ");
        when(aliceAgain.getLastName()).thenReturn(" WONDERLAND ");
        when(aliceAgain.getDisplayName()).thenReturn("Alice bis");

        Participant bob = mock(Participant.class);
        when(bob.getId()).thenReturn(UUID.randomUUID());
        when(bob.getFirstName()).thenReturn("Bob");
        when(bob.getLastName()).thenReturn("Builder");
        when(bob.getDisplayName()).thenReturn("Bob B.");

        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice, aliceAgain, bob));

        Lobby lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        LobbyParticipant aliceEntry = new LobbyParticipant(lobby, alice, now);
        LobbyParticipant bobEntry = new LobbyParticipant(lobby, bob, now);
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId())).thenReturn(List.of(aliceEntry, bobEntry));

        List<LobbyParticipantResponse> views = service.listParticipantViews(eventId);

        assertThat(views).hasSize(2);
        assertThat(views).filteredOn(v -> v.displayName().equals("Alice W.")).first().satisfies(
                v -> assertThat(v.possibleDuplicate()).isTrue());
        assertThat(views).filteredOn(v -> v.displayName().equals("Bob B.")).first().satisfies(
                v -> assertThat(v.possibleDuplicate()).isFalse());
    }

    @Test
    void flagsAParticipantWithMoreThanOneCurrentlyValidSessionAsPossibleQrReuse() {
        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice W.", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice));

        Lobby lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        LobbyParticipant aliceEntry = new LobbyParticipant(lobby, alice, now);
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId())).thenReturn(List.of(aliceEntry));

        AppSession sessionOne = new AppSession(
                ActorType.PARTICIPANT, alice.getId(), null, Role.PARTICIPANT, "hash-1", now.plusSeconds(3600));
        AppSession sessionTwo = new AppSession(
                ActorType.PARTICIPANT, alice.getId(), null, Role.PARTICIPANT, "hash-2", now.plusSeconds(3600));
        when(appSessionRepository.findByParticipantIdAndRevokedAtIsNull(alice.getId()))
                .thenReturn(List.of(sessionOne, sessionTwo));

        List<LobbyParticipantResponse> views = service.listParticipantViews(eventId);

        assertThat(views).singleElement().satisfies(v -> assertThat(v.possibleQrReuse()).isTrue());
    }

    @Test
    void markReadySetsAReadyStatusDistinctFromConnected() {
        UUID participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        when(event.getId()).thenReturn(eventId);
        Participant participant =
                new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        Lobby lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        when(lobbyParticipantRepository.findByLobbyIdAndParticipantId(lobby.getId(), participantId))
                .thenReturn(Optional.empty());
        when(lobbyParticipantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LobbyParticipant result = service.markReady(participantId);

        assertThat(result.getConnectionStatus()).isEqualTo(LobbyConnectionStatus.READY);
    }

    @Test
    void heartbeatDoesNotDowngradeAnAlreadyReadyParticipant() {
        UUID participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        when(event.getId()).thenReturn(eventId);
        Participant participant =
                new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        Lobby lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        LobbyParticipant existing = new LobbyParticipant(lobby, participant, now);
        existing.markReady(now);
        when(lobbyParticipantRepository.findByLobbyIdAndParticipantId(lobby.getId(), participantId))
                .thenReturn(Optional.of(existing));

        LobbyParticipant result = service.heartbeat(participantId);

        assertThat(result.getConnectionStatus()).isEqualTo(LobbyConnectionStatus.READY);
    }
}
