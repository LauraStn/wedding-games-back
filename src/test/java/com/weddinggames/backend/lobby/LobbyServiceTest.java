package com.weddinggames.backend.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.lobby.dto.LobbyParticipantStatusResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the participant-facing lobby status aggregate. */
class LobbyServiceTest {

    private LobbyRepository lobbyRepository;
    private LobbyParticipantRepository lobbyParticipantRepository;
    private ParticipantRepository participantRepository;
    private LobbyService service;

    @BeforeEach
    void setUp() {
        lobbyRepository = mock(LobbyRepository.class);
        lobbyParticipantRepository = mock(LobbyParticipantRepository.class);
        WeddingEventRepository weddingEventRepository = mock(WeddingEventRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new LobbyService(
                lobbyRepository, lobbyParticipantRepository, weddingEventRepository, participantRepository, clock);
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
}
