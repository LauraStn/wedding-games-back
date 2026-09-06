package com.weddinggames.backend.participant;

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
import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingExclusionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test (Mockito, no Spring context) for the participant deletion guard: a participant
 * involved in any HARD pairing exclusion must never be deletable, regardless of caller.
 */
class ParticipantServiceTest {

    private ParticipantRepository participantRepository;
    private PairingExclusionRepository pairingExclusionRepository;
    private AuditLogService auditLogService;
    private ParticipantService service;

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        WeddingEventRepository weddingEventRepository = mock(WeddingEventRepository.class);
        pairingExclusionRepository = mock(PairingExclusionRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new ParticipantService(
                participantRepository, weddingEventRepository, pairingExclusionRepository, auditLogService);
    }

    private Participant mockParticipant(UUID id) {
        Participant participant = mock(Participant.class);
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(UUID.randomUUID());
        when(participant.getEvent()).thenReturn(event);
        when(participantRepository.findById(id)).thenReturn(Optional.of(participant));
        return participant;
    }

    @Test
    void deletingAParticipantInvolvedInAHardExclusionIsRefused() {
        UUID id = UUID.randomUUID();
        mockParticipant(id);
        when(pairingExclusionRepository.existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
                        ExclusionType.HARD, id, ExclusionType.HARD, id))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(id, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exclusion absolue");

        verify(participantRepository, never()).deleteById(id);
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void deletingAParticipantWithoutAnyHardExclusionIsAllowed() {
        UUID id = UUID.randomUUID();
        Participant participant = mockParticipant(id);
        when(pairingExclusionRepository.existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
                        ExclusionType.HARD, id, ExclusionType.HARD, id))
                .thenReturn(false);
        UUID staffAccountId = UUID.randomUUID();

        service.delete(id, staffAccountId);

        verify(auditLogService)
                .record(staffAccountId, AuditAction.PARTICIPANT_DELETED, participant.getEvent().getId(), id, null);

        verify(participantRepository).deleteById(id);
    }

    @Test
    void disablingAParticipantSetsItsStatusToDisabled() {
        UUID id = UUID.randomUUID();
        Participant participant = mock(Participant.class);
        when(participantRepository.findById(id)).thenReturn(Optional.of(participant));

        Participant result = service.disable(id);

        verify(participant).setStatus(ParticipantStatus.DISABLED);
        assertThat(result).isSameAs(participant);
    }

    @Test
    void updatingStatusSetsTheRequestedStatus() {
        UUID id = UUID.randomUUID();
        Participant participant = mock(Participant.class);
        when(participantRepository.findById(id)).thenReturn(Optional.of(participant));

        Participant result = service.updateStatus(id, ParticipantStatus.ABSENT);

        verify(participant).setStatus(ParticipantStatus.ABSENT);
        assertThat(result).isSameAs(participant);
    }

    @Test
    void updatingTableSetsTheRequestedTableLabel() {
        UUID id = UUID.randomUUID();
        Participant participant = mock(Participant.class);
        when(participantRepository.findById(id)).thenReturn(Optional.of(participant));

        Participant result = service.updateTable(id, "Table 7");

        verify(participant).setTableLabel("Table 7");
        assertThat(result).isSameAs(participant);
    }
}
