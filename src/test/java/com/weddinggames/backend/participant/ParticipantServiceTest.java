package com.weddinggames.backend.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
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
    private ParticipantService service;

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        WeddingEventRepository weddingEventRepository = mock(WeddingEventRepository.class);
        pairingExclusionRepository = mock(PairingExclusionRepository.class);
        service = new ParticipantService(participantRepository, weddingEventRepository, pairingExclusionRepository);
    }

    @Test
    void deletingAParticipantInvolvedInAHardExclusionIsRefused() {
        UUID id = UUID.randomUUID();
        when(participantRepository.existsById(id)).thenReturn(true);
        when(pairingExclusionRepository.existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
                        ExclusionType.HARD, id, ExclusionType.HARD, id))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exclusion absolue");

        verify(participantRepository, never()).deleteById(id);
    }

    @Test
    void deletingAParticipantWithoutAnyHardExclusionIsAllowed() {
        UUID id = UUID.randomUUID();
        when(participantRepository.existsById(id)).thenReturn(true);
        when(pairingExclusionRepository.existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
                        ExclusionType.HARD, id, ExclusionType.HARD, id))
                .thenReturn(false);

        service.delete(id);

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
