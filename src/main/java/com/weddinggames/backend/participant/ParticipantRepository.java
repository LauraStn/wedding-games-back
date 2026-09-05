package com.weddinggames.backend.participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByEventId(UUID eventId);

    List<Participant> findByEventIdAndIdIn(UUID eventId, List<UUID> ids);

    Optional<Participant> findByEventIdAndFirstNameAndLastName(UUID eventId, String firstName, String lastName);

    @Query("SELECT p FROM Participant p WHERE p.event.id = :eventId "
            + "AND (:status IS NULL OR p.status = :status) "
            + "AND (:tableLabel IS NULL OR p.tableLabel = :tableLabel) "
            + "AND (:participantType IS NULL OR p.participantType = :participantType) "
            + "AND (:search IS NULL OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    List<Participant> search(
            @Param("eventId") UUID eventId,
            @Param("status") ParticipantStatus status,
            @Param("tableLabel") String tableLabel,
            @Param("participantType") ParticipantType participantType,
            @Param("search") String search);
}
