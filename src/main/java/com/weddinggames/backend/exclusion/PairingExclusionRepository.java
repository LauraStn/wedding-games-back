package com.weddinggames.backend.exclusion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingExclusionRepository extends JpaRepository<PairingExclusion, UUID> {

    List<PairingExclusion> findByEventId(UUID eventId);

    /** Participants must already be passed in normalized order (participantAId &lt; participantBId). */
    Optional<PairingExclusion> findByEventIdAndParticipantAIdAndParticipantBId(
            UUID eventId, UUID participantAId, UUID participantBId);

    boolean existsByEventIdAndParticipantAIdAndParticipantBId(UUID eventId, UUID participantAId, UUID participantBId);

    boolean existsByExclusionTypeAndParticipantAIdOrExclusionTypeAndParticipantBId(
            ExclusionType exclusionTypeA,
            UUID participantAId,
            ExclusionType exclusionTypeB,
            UUID participantBId);
}
