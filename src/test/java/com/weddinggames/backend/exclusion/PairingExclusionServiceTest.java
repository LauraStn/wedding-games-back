package com.weddinggames.backend.exclusion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test (Mockito, no Spring context): the HARD-exclusion delete guard must trigger for
 * every caller, not only when a role check happens to intercept the request first.
 */
class PairingExclusionServiceTest {

    private PairingExclusionRepository pairingExclusionRepository;
    private PairingExclusionService service;

    @BeforeEach
    void setUp() {
        pairingExclusionRepository = mock(PairingExclusionRepository.class);
        ParticipantRepository participantRepository = mock(ParticipantRepository.class);
        WeddingEventRepository weddingEventRepository = mock(WeddingEventRepository.class);
        service = new PairingExclusionService(pairingExclusionRepository, participantRepository, weddingEventRepository);
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

        assertThatThrownBy(() -> service.create(UUID.randomUUID(), request))
                .isInstanceOf(com.weddinggames.backend.common.exception.InvalidRequestException.class);
    }
}
