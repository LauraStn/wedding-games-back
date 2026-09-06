package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.audit.AuditLogService;
import com.weddinggames.backend.common.exception.InvalidInvitationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
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
    private AuditLogService auditLogService;
    private InvitationService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        InvitationProperties properties = new InvitationProperties();
        properties.setBaseUrl("https://example.test/invite");
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        auditLogService = mock(AuditLogService.class);
        service = new InvitationService(invitationRepository, participantRepository, properties, clock, auditLogService);
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

        List<InvitationPrintCard> cards = service.generateBatch(eventId, null, UUID.randomUUID());

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).displayName()).isEqualTo("Alice Wonderland");
        assertThat(cards.get(0).tableLabel()).isEqualTo("Table 5");
        assertThat(cards.get(0).invitationUrl()).startsWith("https://example.test/invite/");
        assertThat(cards.get(1).tableLabel()).isNull();
        verify(invitationRepository, times(2)).save(any());
    }

    @Test
    void batchGenerationIsAuditLoggedOnceForTheWholeBatch() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Participant bob = new Participant(event, "Bob", "Builder", "Bob Builder", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice, bob));
        UUID staffAccountId = UUID.randomUUID();

        service.generateBatch(eventId, null, staffAccountId);

        verify(auditLogService)
                .record(
                        eq(staffAccountId),
                        eq(com.weddinggames.backend.common.audit.AuditAction.INVITATION_BATCH_REGENERATED),
                        eq(eventId),
                        org.mockito.ArgumentMatchers.isNull(),
                        anyString());
    }

    @Test
    void restrictsGenerationToTheGivenParticipantIds() {
        UUID aliceId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findByEventIdAndIdIn(eventId, List.of(aliceId))).thenReturn(List.of(alice));

        List<InvitationPrintCard> cards = service.generateBatch(eventId, List.of(aliceId), UUID.randomUUID());

        assertThat(cards).hasSize(1);
        verify(participantRepository).findByEventIdAndIdIn(eventId, List.of(aliceId));
    }

    @Test
    void rejectsBatchGenerationWhenNoParticipantMatches() {
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateBatch(eventId, null, UUID.randomUUID())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void revokesAnyCurrentlyActiveInvitationBeforeIssuingANewOne() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation activeInvitation = new Invitation(alice, "old-hash");
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice));
        when(invitationRepository.findByParticipantIdAndStatus(alice.getId(), InvitationStatus.ACTIVE))
                .thenReturn(List.of(activeInvitation));

        service.generateBatch(eventId, null, UUID.randomUUID());

        assertThat(activeInvitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    void revokeInvalidatesTheActiveInvitationWithoutCreatingANewOne() {
        UUID participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation activeInvitation = new Invitation(participant, "some-hash");
        when(invitationRepository.findByParticipantIdAndStatus(participantId, InvitationStatus.ACTIVE))
                .thenReturn(List.of(activeInvitation));

        service.revoke(participantId);

        assertThat(activeInvitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(activeInvitation.getRevokedAt()).isNotNull();
        verify(invitationRepository, times(0)).save(any());
    }

    @Test
    void revokeFailsWhenThereIsNoActiveInvitation() {
        UUID participantId = UUID.randomUUID();
        when(invitationRepository.findByParticipantIdAndStatus(participantId, InvitationStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.revoke(participantId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generationAssignsASixCharacterFallbackCode() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice));

        List<InvitationPrintCard> cards = service.generateBatch(eventId, null, UUID.randomUUID());

        assertThat(cards).hasSize(1);
        org.mockito.ArgumentCaptor<Invitation> captor = org.mockito.ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getFallbackCode()).hasSize(6);
    }

    @Test
    void regeneratesTheFallbackCodeOnCollisionUntilAUniqueOneIsFound() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant alice = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(alice));
        when(invitationRepository.existsByFallbackCode(anyString())).thenReturn(true, true, false);

        service.generateBatch(eventId, null, UUID.randomUUID());

        verify(invitationRepository, times(3)).existsByFallbackCode(anyString());
    }

    @Test
    void renewFallbackCodeReplacesOnlyTheCodeOfTheActiveInvitation() {
        UUID participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation activeInvitation = new Invitation(participant, "some-hash");
        activeInvitation.setFallbackCode("OLDCOD");
        when(invitationRepository.findByParticipantIdAndStatus(participantId, InvitationStatus.ACTIVE))
                .thenReturn(List.of(activeInvitation));

        String newCode = service.renewFallbackCode(participantId);

        assertThat(newCode).isNotEqualTo("OLDCOD").hasSize(6);
        assertThat(activeInvitation.getFallbackCode()).isEqualTo(newCode);
        assertThat(activeInvitation.getTokenHash()).isEqualTo("some-hash");
    }

    @Test
    void renewFallbackCodeFailsWhenThereIsNoActiveInvitation() {
        UUID participantId = UUID.randomUUID();
        when(invitationRepository.findByParticipantIdAndStatus(participantId, InvitationStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.renewFallbackCode(participantId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolveByFallbackCodeReturnsTheParticipantOfAnActiveInvitation() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation invitation = new Invitation(participant, "some-hash");
        invitation.setFallbackCode("ABC234");
        when(invitationRepository.findByFallbackCode("ABC234")).thenReturn(java.util.Optional.of(invitation));

        assertThat(service.resolveByFallbackCode("ABC234")).isSameAs(participant);
    }

    @Test
    void resolveByFallbackCodeRejectsAnUnknownCode() {
        when(invitationRepository.findByFallbackCode("NOPE12")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.resolveByFallbackCode("NOPE12"))
                .isInstanceOf(InvalidInvitationException.class);
    }

    @Test
    void resolveByFallbackCodeRejectsARevokedInvitation() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation invitation = new Invitation(participant, "some-hash");
        invitation.setFallbackCode("ABC234");
        invitation.revoke(Instant.parse("2026-01-01T00:00:00Z"));
        when(invitationRepository.findByFallbackCode("ABC234")).thenReturn(java.util.Optional.of(invitation));

        assertThatThrownBy(() -> service.resolveByFallbackCode("ABC234"))
                .isInstanceOf(InvalidInvitationException.class);
    }

    @Test
    void confirmByFallbackCodeConnectsTheParticipant() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Alice", "Wonderland", "Alice Wonderland", null, ParticipantType.GUEST);
        Invitation invitation = new Invitation(participant, "some-hash");
        invitation.setFallbackCode("ABC234");
        when(invitationRepository.findByFallbackCode("ABC234")).thenReturn(java.util.Optional.of(invitation));

        Participant result = service.confirmByFallbackCode("ABC234");

        assertThat(result.getStatus())
                .isEqualTo(com.weddinggames.backend.participant.ParticipantStatus.CONNECTED);
    }
}
