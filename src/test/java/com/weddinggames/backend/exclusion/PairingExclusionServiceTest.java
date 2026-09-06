package com.weddinggames.backend.exclusion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.audit.AuditAction;
import com.weddinggames.backend.common.audit.AuditLogService;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.dto.PairingExclusionCreateRequest;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test (Mockito, no Spring context): the HARD-exclusion delete guard must trigger for
 * every caller, not only when a role check happens to intercept the request first; also covers
 * that only HARD exclusion mutations are audit-logged (ASST-123), never PREFERENCE ones.
 */
class PairingExclusionServiceTest {

    private PairingExclusionRepository pairingExclusionRepository;
    private ParticipantRepository participantRepository;
    private WeddingEventRepository weddingEventRepository;
    private AuditLogService auditLogService;
    private PairingExclusionService service;

    @BeforeEach
    void setUp() {
        pairingExclusionRepository = mock(PairingExclusionRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        weddingEventRepository = mock(WeddingEventRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new PairingExclusionService(
                pairingExclusionRepository, participantRepository, weddingEventRepository, auditLogService);
        when(pairingExclusionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Participant mockParticipant(UUID id) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(id);
        when(participant.getDisplayName()).thenReturn("Participant-" + id);
        when(participantRepository.findById(id)).thenReturn(Optional.of(participant));
        return participant;
    }

    @Test
    void deletingAHardExclusionIsAlwaysRefused() {
        UUID id = UUID.randomUUID();
        PairingExclusion hardExclusion = mock(PairingExclusion.class);
        when(hardExclusion.getExclusionType()).thenReturn(ExclusionType.HARD);
        when(pairingExclusionRepository.findById(id)).thenReturn(java.util.Optional.of(hardExclusion));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("HARD");

        verify(pairingExclusionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletingAPreferenceExclusionIsAllowed() {
        UUID id = UUID.randomUUID();
        PairingExclusion preferenceExclusion = mock(PairingExclusion.class);
        when(preferenceExclusion.getExclusionType()).thenReturn(ExclusionType.PREFERENCE);
        when(pairingExclusionRepository.findById(id)).thenReturn(java.util.Optional.of(preferenceExclusion));

        service.delete(id);

        verify(pairingExclusionRepository).delete(preferenceExclusion);
    }

    @Test
    void createRejectsSelfExclusion() {
        UUID sameId = UUID.randomUUID();
        var request = new com.weddinggames.backend.exclusion.dto.PairingExclusionCreateRequest(
                sameId, sameId, "motif", ExclusionType.HARD);

        assertThatThrownBy(() -> service.create(UUID.randomUUID(), request, UUID.randomUUID()))
                .isInstanceOf(com.weddinggames.backend.common.exception.InvalidRequestException.class);
    }

    @Test
    void creatingAHardExclusionIsAuditLogged() {
        UUID eventId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        mockParticipant(aId);
        mockParticipant(bId);
        UUID staffAccountId = UUID.randomUUID();
        var request = new PairingExclusionCreateRequest(aId, bId, "motif", ExclusionType.HARD);

        PairingExclusion created = service.create(eventId, request, staffAccountId);

        verify(auditLogService)
                .record(
                        org.mockito.ArgumentMatchers.eq(staffAccountId),
                        org.mockito.ArgumentMatchers.eq(AuditAction.HARD_EXCLUSION_CREATED),
                        org.mockito.ArgumentMatchers.eq(eventId),
                        org.mockito.ArgumentMatchers.eq(created.getId()),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void creatingAPreferenceExclusionIsNotAuditLogged() {
        UUID eventId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        mockParticipant(aId);
        mockParticipant(bId);
        var request = new PairingExclusionCreateRequest(aId, bId, "motif", ExclusionType.PREFERENCE);

        service.create(eventId, request, UUID.randomUUID());

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updatingTheReasonOfAHardExclusionIsAuditLogged() {
        UUID id = UUID.randomUUID();
        PairingExclusion hardExclusion = mock(PairingExclusion.class);
        when(hardExclusion.getId()).thenReturn(id);
        when(hardExclusion.getExclusionType()).thenReturn(ExclusionType.HARD);
        WeddingEvent event = mock(WeddingEvent.class);
        UUID eventId = UUID.randomUUID();
        when(event.getId()).thenReturn(eventId);
        when(hardExclusion.getEvent()).thenReturn(event);
        when(pairingExclusionRepository.findById(id)).thenReturn(Optional.of(hardExclusion));
        UUID staffAccountId = UUID.randomUUID();

        service.updateReason(id, "Nouveau motif", staffAccountId);

        verify(auditLogService)
                .record(staffAccountId, AuditAction.HARD_EXCLUSION_REASON_UPDATED, eventId, id, "Nouveau motif");
    }

    @Test
    void updatingTheReasonOfAPreferenceExclusionIsNotAuditLogged() {
        UUID id = UUID.randomUUID();
        PairingExclusion preferenceExclusion = mock(PairingExclusion.class);
        when(preferenceExclusion.getExclusionType()).thenReturn(ExclusionType.PREFERENCE);
        when(pairingExclusionRepository.findById(id)).thenReturn(Optional.of(preferenceExclusion));

        service.updateReason(id, "Nouveau motif", UUID.randomUUID());

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }
}
