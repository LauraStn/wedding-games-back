package com.weddinggames.backend.participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByEventId(UUID eventId);

    Optional<Participant> findByEventIdAndFirstNameAndLastName(UUID eventId, String firstName, String lastName);
}
