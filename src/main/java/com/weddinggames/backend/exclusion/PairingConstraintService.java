package com.weddinggames.backend.exclusion;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic matchmaking constraint checker, independent of any concrete pairing algorithm
 * (which will be built in a later phase). Never compares participant names: exclusions are
 * looked up strictly by participant UUID pair.
 */
@Service
public class PairingConstraintService {

    private final PairingExclusionRepository pairingExclusionRepository;

    public PairingConstraintService(PairingExclusionRepository pairingExclusionRepository) {
        this.pairingExclusionRepository = pairingExclusionRepository;
    }

    /** False only when an absolute (HARD) exclusion exists between the two participants. */
    @Transactional(readOnly = true)
    public boolean canPair(UUID eventId, UUID participantAId, UUID participantBId) {
        return findExclusion(eventId, participantAId, participantBId)
                .map(exclusion -> exclusion.getExclusionType() != ExclusionType.HARD)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean hasHardExclusion(UUID eventId, UUID participantAId, UUID participantBId) {
        return findExclusion(eventId, participantAId, participantBId)
                .map(exclusion -> exclusion.getExclusionType() == ExclusionType.HARD)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<PairingExclusion> findExclusion(UUID eventId, UUID participantAId, UUID participantBId) {
        UUID lowId = ExclusionPair.lower(participantAId, participantBId);
        UUID highId = ExclusionPair.higher(participantAId, participantBId);
        return pairingExclusionRepository.findByEventIdAndParticipantAIdAndParticipantBId(eventId, lowId, highId);
    }
}
