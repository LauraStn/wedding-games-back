package com.weddinggames.backend.lobby;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LobbyParticipantRepository extends JpaRepository<LobbyParticipant, UUID> {

    List<LobbyParticipant> findByLobbyId(UUID lobbyId);

    Optional<LobbyParticipant> findByLobbyIdAndParticipantId(UUID lobbyId, UUID participantId);

    long countByLobbyIdAndConnectionStatus(UUID lobbyId, LobbyConnectionStatus connectionStatus);
}
