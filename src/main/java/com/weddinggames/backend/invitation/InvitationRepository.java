package com.weddinggames.backend.invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByFallbackCode(String fallbackCode);

    boolean existsByFallbackCode(String fallbackCode);

    List<Invitation> findByParticipantIdAndStatus(UUID participantId, InvitationStatus status);
}
