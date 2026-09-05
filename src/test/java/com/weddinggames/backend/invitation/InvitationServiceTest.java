package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the batch invitation generation logic. */
class InvitationServiceTest {

    private InvitationRepository invitationRepository;
    private ParticipantRepository participantRepository;
    private InvitationService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        InvitationProperties properties = new InvitationProperties();
        properties.setBaseUrl("https://example.test/invite");
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new InvitationService(invitationRepository, participantRepository, properties, clock);
        eventId = UUID.randomUUID();

        when(invitationRepository.findByParticipantIdAndStatus(any(), eq(InvitationStatus.ACTIVE)))
                .thenReturn(List.of());
    }

    @Test
    void generatesOneInvitationPerParticipantWhenNoIdsAreGiven() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", "Table 5", ParticipantType.GUEST);
        Participant bob = new Participant(event, "Bob", "Builder", "Bob Builder", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice, bob));

        List<InvitationPrintCard> cards = service.generateBatch(eventId, null);

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).displayName()).isEqualTo("Alice Wonderland");
        assertThat(cards.get(0).tableLabel()).isEqualTo("Table 5");
        assertThat(cards.get(0).invitationUrl()).startsWith("https://example.test/invite/");
        assertThat(cards.get(1).tableLabel()).isNull();
        verify(invitationRepository, times(2)).save(any());
    }

    @Test
    void restrictsGenerationToTheGivenParticipantIds() {
        UUID aliceId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findByEventIdAndIdIn(eventId, List.of(aliceId))).thenReturn(List.of(alice));

        List<InvitationPrintCard> cards = service.generateBatch(eventId, List.of(aliceId));

        assertThat(cards).hasSize(1);
        verify(participantRepository).findByEventIdAndIdIn(eventId, List.of(aliceId));
    }

    @Test
    void rejectsBatchGenerationWhenNoParticipantMatches() {
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateBatch(eventId, null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void revokesAnyCurrentlyActiveInvitationBeforeIssuingANewOne() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation activeInvitation = new Invitation(alice, "old-hash");
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice));
        when(invitationRepository.findByParticipantIdAndStatus(alice.getId(), InvitationStatus.ACTIVE))
                .thenReturn(List.of(activeInvitation));

        service.generateBatch(eventId, null);

        assertThat(activeInvitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }
}
