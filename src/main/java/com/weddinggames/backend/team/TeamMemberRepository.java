package com.weddinggames.backend.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeamId(UUID teamId);

    List<TeamMember> findByTeamIdIn(List<UUID> teamIds);

    Optional<TeamMember> findByParticipantId(UUID participantId);

    boolean existsByParticipantId(UUID participantId);

    boolean existsByCharacterId(UUID characterId);
}
